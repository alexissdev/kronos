package dev.alexissdev.kronos.players.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.domain.CrateLocation;
import dev.alexissdev.kronos.players.repository.CrateLocationRepository;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class MongoCrateLocationRepository implements CrateLocationRepository {

    private static final String COLLECTION = "crate_locations";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    @Inject
    public MongoCrateLocationRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<Optional<CrateLocation>> findByLocation(String world, int x, int y, int z) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(
                    Filters.and(
                            Filters.eq("world", world),
                            Filters.eq("x", x),
                            Filters.eq("y", y),
                            Filters.eq("z", z)
                    )).first();
            return Optional.ofNullable(doc).map(this::toLocation);
        }, executor);
    }

    @Override
    public CompletableFuture<List<CrateLocation>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<CrateLocation> result = new ArrayList<>();
            collection.find().forEach(doc -> result.add(toLocation(doc)));
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<CrateLocation> save(CrateLocation location) {
        return CompletableFuture.supplyAsync(() -> {
            collection.replaceOne(
                    Filters.eq("_id", location.getId()),
                    toDocument(location),
                    new ReplaceOptions().upsert(true));
            return location;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", id)), executor);
    }

    private CrateLocation toLocation(Document doc) {
        CrateType type = CrateType.KOTH;
        try { type = CrateType.valueOf(doc.getString("type")); } catch (Exception ignored) {}
        return new CrateLocation(
                doc.getString("_id"),
                doc.getString("world"),
                doc.getInteger("x", 0),
                doc.getInteger("y", 0),
                doc.getInteger("z", 0),
                type);
    }

    private Document toDocument(CrateLocation loc) {
        return new Document()
                .append("_id",   loc.getId())
                .append("world", loc.getWorld())
                .append("x",     loc.getX())
                .append("y",     loc.getY())
                .append("z",     loc.getZ())
                .append("type",  loc.getType().name());
    }
}
