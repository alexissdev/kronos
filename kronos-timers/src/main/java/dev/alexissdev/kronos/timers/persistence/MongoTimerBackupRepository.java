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
 * MongoDB backup repository for active player timers.
 *
 * <p>Acts as a secondary durability layer to ensure timers are not lost in the event
 * of a Redis restart or a server crash with persistence disabled.
 * Writes are fire-and-forget (the caller does not wait for the result) to avoid
 * adding latency to the main game thread. Reads only occur during player login,
 * when Redis returns no active timer and MongoDB is consulted as a fallback.</p>
 *
 * <p>Timers are stored in the {@code player_timers} collection using a composite ID
 * of the form {@code {uuid}:{timerType}} as the primary key. Unlike Redis, MongoDB
 * does not automatically remove expired documents, so expiration is checked at read
 * time by comparing the {@code expiresAt} field against the current instant.</p>
 */
@Singleton
public class MongoTimerBackupRepository {

    private static final String COLLECTION = "player_timers";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    /**
     * Creates the backup repository by obtaining the MongoDB collection
     * through the connection factory.
     *
     * @param factory factory that provides the MongoDB connection and database
     */
    @Inject
    public MongoTimerBackupRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    /**
     * Saves or updates the timer in MongoDB as a backup copy (upsert).
     *
     * <p>The operation runs asynchronously without blocking the caller.
     * The document ID is formed by combining the player UUID with the timer type name,
     * guaranteeing uniqueness per player and type combination.</p>
     *
     * @param timer {@link Timer} entity to persist as a backup in MongoDB
     * @return future that resolves once the document has been saved in MongoDB
     */
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

    /**
     * Looks up a player's backup timer in MongoDB.
     * Used as a fallback during login when Redis does not contain the active timer
     * (for example, after a Redis restart with persistence disabled).
     *
     * <p>If the document exists but its expiration instant has already passed, an empty
     * {@link Optional} is returned to prevent restoring already-expired timers.</p>
     *
     * @param playerUuid UUID of the player whose backup timer is being searched
     * @param type       type of the timer to look up
     * @return future that resolves with an {@link Optional} containing the timer
     *         if it exists in MongoDB and has not expired, or empty if absent or expired
     */
    public CompletableFuture<Optional<Timer>> find(UUID playerUuid, TimerType type) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", docId(playerUuid, type))).first();
            if (doc == null) return Optional.empty();
            long expiresAtMs = doc.getLong("expiresAt");
            Timer timer = new Timer(playerUuid, type, Instant.ofEpochMilli(expiresAtMs));
            return timer.isExpired() ? Optional.empty() : Optional.of(timer);
        }, executor);
    }

    /**
     * Deletes the backup document for a player's timer from MongoDB.
     * Called when a timer is cancelled to keep Redis and MongoDB in sync.
     *
     * @param playerUuid UUID of the player whose backup timer should be removed
     * @param type       type of the timer to delete from the backup
     * @return future that resolves once the document has been deleted from MongoDB
     */
    public CompletableFuture<Void> delete(UUID playerUuid, TimerType type) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", docId(playerUuid, type))), executor);
    }

    /**
     * Builds the composite MongoDB document ID for a player's timer.
     * The format is {@code {uuid}:{timerType}}, guaranteeing uniqueness per player and type.
     *
     * @param uuid UUID of the player who owns the timer
     * @param type type of the timer
     * @return document ID in the format {@code uuid:timerType}
     */
    private static String docId(UUID uuid, TimerType type) {
        return uuid.toString() + ":" + type.name();
    }
}
