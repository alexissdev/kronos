package dev.alexissdev.kronos.spawn.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.repository.SpawnRepository;
import org.bson.Document;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class MongoSpawnRepository implements SpawnRepository {

    private static final String COLLECTION = "spawn";
    private static final String DOC_ID     = "zone";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    @Inject
    public MongoSpawnRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<Optional<SpawnZone>> findZone() {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", DOC_ID)).first();
            if (doc == null) return Optional.empty();
            return Optional.of(new SpawnZone(
                    doc.getString("world"),
                    doc.getInteger("minX", 0),
                    doc.getInteger("minZ", 0),
                    doc.getInteger("maxX", 0),
                    doc.getInteger("maxZ", 0)
            ));
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveZone(SpawnZone zone) {
        return CompletableFuture.runAsync(() -> {
            Document doc = new Document()
                    .append("_id",   DOC_ID)
                    .append("world", zone.getWorld())
                    .append("minX",  zone.getMinX())
                    .append("minZ",  zone.getMinZ())
                    .append("maxX",  zone.getMaxX())
                    .append("maxZ",  zone.getMaxZ());
            collection.replaceOne(Filters.eq("_id", DOC_ID), doc, new ReplaceOptions().upsert(true));
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteZone() {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", DOC_ID)), executor);
    }
}
