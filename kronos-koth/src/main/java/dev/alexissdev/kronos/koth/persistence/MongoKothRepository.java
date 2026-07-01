package dev.alexissdev.kronos.koth.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.repository.KothRepository;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class MongoKothRepository implements KothRepository {

    private static final String COLLECTION = "koths";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    @Inject
    public MongoKothRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<Optional<KothZone>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", name)).first();
            return Optional.ofNullable(doc).map(this::toZone);
        }, executor);
    }

    @Override
    public CompletableFuture<List<KothZone>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<KothZone> result = new ArrayList<>();
            collection.find().forEach(doc -> result.add(toZone(doc)));
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<KothZone> save(KothZone zone) {
        return CompletableFuture.supplyAsync(() -> {
            collection.replaceOne(
                    Filters.eq("_id", zone.getName()),
                    toDocument(zone),
                    new ReplaceOptions().upsert(true));
            return zone;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String name) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", name)), executor);
    }

    private KothZone toZone(Document doc) {
        int minX = doc.getInteger("minX", 0);
        int minZ = doc.getInteger("minZ", 0);
        int maxX = doc.getInteger("maxX", 0);
        int maxZ = doc.getInteger("maxZ", 0);

        // Fall back to claim zone if capture zone was not stored (legacy data)
        int capMinX = doc.getInteger("captureMinX", minX);
        int capMinZ = doc.getInteger("captureMinZ", minZ);
        int capMaxX = doc.getInteger("captureMaxX", maxX);
        int capMaxZ = doc.getInteger("captureMaxZ", maxZ);

        CrateType crateType = parseCrateType(doc.getString("rewardCrateType"));

        KothZone zone = new KothZone(
                doc.getString("_id"),
                doc.getString("world"),
                minX, minZ, maxX, maxZ,
                capMinX, capMinZ, capMaxX, capMaxZ,
                doc.getInteger("captureTimeSeconds", 300),
                crateType
        );
        zone.setActive(doc.getBoolean("active", false));
        return zone;
    }

    private Document toDocument(KothZone z) {
        return new Document()
                .append("_id",               z.getName())
                .append("world",             z.getWorld())
                .append("minX",              z.getMinX())
                .append("minZ",              z.getMinZ())
                .append("maxX",              z.getMaxX())
                .append("maxZ",              z.getMaxZ())
                .append("captureMinX",       z.getCaptureMinX())
                .append("captureMinZ",       z.getCaptureMinZ())
                .append("captureMaxX",       z.getCaptureMaxX())
                .append("captureMaxZ",       z.getCaptureMaxZ())
                .append("captureTimeSeconds",z.getCaptureTimeSeconds())
                .append("rewardCrateType",   z.getRewardCrateType().name())
                .append("active",            z.isActive());
    }

    private static CrateType parseCrateType(String value) {
        if (value == null) return CrateType.KOTH;
        try { return CrateType.valueOf(value); }
        catch (IllegalArgumentException e) { return CrateType.KOTH; }
    }
}
