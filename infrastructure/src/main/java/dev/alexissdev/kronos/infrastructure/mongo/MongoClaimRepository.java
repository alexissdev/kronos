package dev.alexissdev.kronos.infrastructure.mongo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.domain.Claim;
import dev.alexissdev.kronos.core.domain.ClaimType;
import dev.alexissdev.kronos.core.repository.ClaimRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class MongoClaimRepository implements ClaimRepository {

    private static final String COLLECTION = "claims";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    @Inject
    public MongoClaimRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<Optional<Claim>> findByChunk(String world, int chunkX, int chunkZ) {
        return CompletableFuture.supplyAsync(() -> {
            Bson filter = Filters.and(
                    Filters.eq("world", world),
                    Filters.lte("minChunkX", chunkX),
                    Filters.gte("maxChunkX", chunkX),
                    Filters.lte("minChunkZ", chunkZ),
                    Filters.gte("maxChunkZ", chunkZ)
            );
            Document doc = collection.find(filter).first();
            return Optional.ofNullable(doc).map(this::toClaim);
        }, executor);
    }

    @Override
    public CompletableFuture<List<Claim>> findByFaction(String factionId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Claim> result = new ArrayList<>();
            collection.find(Filters.eq("factionId", factionId))
                    .forEach(doc -> result.add(toClaim(doc)));
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<List<Claim>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<Claim> result = new ArrayList<>();
            collection.find().forEach(doc -> result.add(toClaim(doc)));
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Claim> save(Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            collection.replaceOne(
                    Filters.eq("_id", claim.getId()),
                    toDocument(claim),
                    new ReplaceOptions().upsert(true)
            );
            return claim;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteByFaction(String factionId) {
        return CompletableFuture.runAsync(
                () -> collection.deleteMany(Filters.eq("factionId", factionId)), executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", id)), executor);
    }

    private Claim toClaim(Document doc) {
        String typeStr = doc.getString("type");
        ClaimType type;
        try {
            type = typeStr != null ? ClaimType.valueOf(typeStr) : ClaimType.FACTION;
        } catch (IllegalArgumentException e) {
            type = ClaimType.FACTION;
        }
        Integer minX = doc.getInteger("minChunkX");
        Integer minZ = doc.getInteger("minChunkZ");
        Integer maxX = doc.getInteger("maxChunkX");
        Integer maxZ = doc.getInteger("maxChunkZ");
        return new Claim(
                doc.getString("_id"),
                doc.getString("factionId"),
                type,
                doc.getString("world"),
                minX != null ? minX : 0,
                minZ != null ? minZ : 0,
                maxX != null ? maxX : 0,
                maxZ != null ? maxZ : 0
        );
    }

    private Document toDocument(Claim c) {
        return new Document()
                .append("_id", c.getId())
                .append("factionId", c.getFactionId())
                .append("type", c.getType().name())
                .append("world", c.getWorld())
                .append("minChunkX", c.getMinChunkX())
                .append("minChunkZ", c.getMinChunkZ())
                .append("maxChunkX", c.getMaxChunkX())
                .append("maxChunkZ", c.getMaxChunkZ());
    }
}
