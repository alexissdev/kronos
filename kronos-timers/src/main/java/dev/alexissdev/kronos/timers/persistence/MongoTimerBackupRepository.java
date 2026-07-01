package dev.alexissdev.kronos.timers.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.timers.domain.Timer;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bson.Document;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * MongoDB backup for timers — used as fallback when Redis loses data
 * (e.g. Redis restart without persistence). Writes are fire-and-forget;
 * reads only happen when Redis returns empty on player login.
 */
@Singleton
public class MongoTimerBackupRepository {

    private static final String COLLECTION = "player_timers";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    @Inject
    public MongoTimerBackupRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    public CompletableFuture<Void> save(Timer timer) {
        return CompletableFuture.runAsync(() -> {
            Document doc = new Document("_id", docId(timer.getPlayerUuid(), timer.getType()))
                    .append("playerUuid", timer.getPlayerUuid().toString())
                    .append("timerType",  timer.getType().name())
                    .append("expiresAt",  timer.getExpiresAt().toEpochMilli());
            collection.replaceOne(
                    Filters.eq("_id", docId(timer.getPlayerUuid(), timer.getType())),
                    doc,
                    new ReplaceOptions().upsert(true));
        }, executor);
    }

    public CompletableFuture<Optional<Timer>> find(UUID playerUuid, TimerType type) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", docId(playerUuid, type))).first();
            if (doc == null) return Optional.empty();
            long expiresAtMs = doc.getLong("expiresAt");
            Timer timer = new Timer(playerUuid, type, Instant.ofEpochMilli(expiresAtMs));
            return timer.isExpired() ? Optional.empty() : Optional.of(timer);
        }, executor);
    }

    public CompletableFuture<Void> delete(UUID playerUuid, TimerType type) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", docId(playerUuid, type))), executor);
    }

    private static String docId(UUID uuid, TimerType type) {
        return uuid.toString() + ":" + type.name();
    }
}
