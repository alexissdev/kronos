package dev.alexissdev.kronos.players.persistence;

import dev.alexissdev.kronos.common.database.MongoConnectionFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.domain.HCFPlayer;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class MongoPlayerRepository implements PlayerRepository {

    private static final String COLLECTION = "players";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    @Inject
    public MongoPlayerRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<Optional<HCFPlayer>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", uuid.toString())).first();
            return Optional.ofNullable(doc).map(this::toPlayer);
        }, executor);
    }

    @Override
    public CompletableFuture<HCFPlayer> save(HCFPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            collection.replaceOne(
                    Filters.eq("_id", player.getUuid().toString()),
                    toDocument(player),
                    new ReplaceOptions().upsert(true)
            );
            return player;
        }, executor);
    }

    private HCFPlayer toPlayer(Document doc) {
        KitType kit = KitType.DIAMOND;
        String kitStr = doc.getString("activeKit");
        if (kitStr != null) {
            try { kit = KitType.valueOf(kitStr); } catch (IllegalArgumentException ignored) {}
        }
        return new HCFPlayer(
                UUID.fromString(doc.getString("_id")),
                doc.getString("name"),
                doc.getInteger("kills", 0),
                doc.getInteger("deaths", 0),
                kit,
                doc.getString("savedInventoryJson"),
                doc.getInteger("lives", 3),
                doc.getBoolean("pvpTimerGiven", false),
                doc.getLong("lastLifeRegenAt") != null ? doc.getLong("lastLifeRegenAt") : System.currentTimeMillis()
        );
    }

    private Document toDocument(HCFPlayer p) {
        return new Document()
                .append("_id", p.getUuid().toString())
                .append("name", p.getName())
                .append("kills", p.getKills())
                .append("deaths", p.getDeaths())
                .append("lives", p.getLives())
                .append("pvpTimerGiven", p.isPvpTimerGiven())
                .append("activeKit", p.getActiveKit().name())
                .append("savedInventoryJson", p.getSavedInventoryJson())
                .append("lastLifeRegenAt", p.getLastLifeRegenAt());
    }
}
