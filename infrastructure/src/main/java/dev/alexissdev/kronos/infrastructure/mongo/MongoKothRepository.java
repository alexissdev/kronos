package dev.alexissdev.kronos.infrastructure.mongo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.domain.CrateType;
import dev.alexissdev.kronos.core.domain.KothZone;
import dev.alexissdev.kronos.core.repository.KothRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
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
        this.executor = Executors.newCachedThreadPool();
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
                    new ReplaceOptions().upsert(true)
            );
            return zone;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String name) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", name)), executor);
    }

    private KothZone toZone(Document doc) {
        Integer minX = doc.getInteger("minX");
        Integer minZ = doc.getInteger("minZ");
        Integer maxX = doc.getInteger("maxX");
        Integer maxZ = doc.getInteger("maxZ");
        String crateTypeStr = doc.getString("rewardCrateType");
        CrateType crateType;
        try {
            crateType = crateTypeStr != null ? CrateType.valueOf(crateTypeStr) : CrateType.KOTH;
        } catch (IllegalArgumentException e) {
            crateType = CrateType.KOTH;
        }
        KothZone zone = new KothZone(
                doc.getString("_id"),
                doc.getString("world"),
                minX != null ? minX : 0,
                minZ != null ? minZ : 0,
                maxX != null ? maxX : 0,
                maxZ != null ? maxZ : 0,
                doc.getInteger("captureTimeSeconds", 300),
                crateType
        );
        zone.setActive(doc.getBoolean("active", false));
        return zone;
    }

    private Document toDocument(KothZone z) {
        return new Document()
                .append("_id", z.getName())
                .append("world", z.getWorld())
                .append("minX", z.getMinX())
                .append("minZ", z.getMinZ())
                .append("maxX", z.getMaxX())
                .append("maxZ", z.getMaxZ())
                .append("captureTimeSeconds", z.getCaptureTimeSeconds())
                .append("rewardCrateType", z.getRewardCrateType().name())
                .append("active", z.isActive());
    }
}
