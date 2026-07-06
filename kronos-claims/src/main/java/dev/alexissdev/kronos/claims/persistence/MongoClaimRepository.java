package dev.alexissdev.kronos.claims.persistence;

import dev.alexissdev.kronos.common.database.MongoConnectionFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.claims.repository.ClaimRepository;
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

/**
 * MongoDB implementation of {@link ClaimRepository}.
 *
 * <p>Persists and retrieves {@link Claim} entities from the {@code "claims"} MongoDB
 * collection. All operations run on an independent thread pool
 * ({@link Executors#newCachedThreadPool()}) to avoid blocking the main Bukkit server
 * thread.</p>
 *
 * <p>Chunk lookups use range filters on the {@code minChunkX/maxChunkX} and
 * {@code minChunkZ/maxChunkZ} fields, so compound indexes on those fields are
 * recommended in production to ensure adequate query performance.</p>
 */
@Singleton
public class MongoClaimRepository implements ClaimRepository {

    private static final String COLLECTION = "claims";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    /**
     * Constructs the repository by obtaining the claims collection from the database.
     *
     * @param factory MongoDB connection factory injected by Guice, providing access
     *                to the database configured for the plugin
     */
    @Inject
    public MongoClaimRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes a range query in MongoDB to find the claim whose chunk rectangle
     * contains the given coordinates.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves all documents whose {@code factionId} field matches the given identifier.</p>
     */
    @Override
    public CompletableFuture<List<Claim>> findByFaction(String factionId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Claim> result = new ArrayList<>();
            collection.find(Filters.eq("factionId", factionId))
                    .forEach(doc -> result.add(toClaim(doc)));
            return result;
        }, executor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Iterates over every document in the {@code claims} collection and maps them to
     * domain entities. Intended for the initial population of the in-memory cache at
     * server startup.</p>
     */
    @Override
    public CompletableFuture<List<Claim>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<Claim> result = new ArrayList<>();
            collection.find().forEach(doc -> result.add(toClaim(doc)));
            return result;
        }, executor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs an upsert keyed on the claim's {@code _id} field. If the document does
     * not exist it is inserted; if it already exists it is fully replaced.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Bulk-deletes all documents whose {@code factionId} field matches. Called when a
     * faction is disbanded to release all of its territory at once.</p>
     */
    @Override
    public CompletableFuture<Void> deleteByFaction(String factionId) {
        return CompletableFuture.runAsync(
                () -> collection.deleteMany(Filters.eq("factionId", factionId)), executor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deletes the document whose {@code _id} field matches the given identifier.</p>
     */
    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", id)), executor);
    }

    /**
     * Converts a MongoDB BSON document into a {@link Claim} domain entity.
     *
     * <p>If the {@code type} field contains an unknown or null value, {@link ClaimType#FACTION}
     * is assumed to maintain backward compatibility with legacy data. Null chunk coordinates
     * are normalised to {@code 0}.</p>
     *
     * @param doc MongoDB document to convert
     * @return {@link Claim} entity built from the document
     */
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

    /**
     * Converts a {@link Claim} domain entity into a BSON document for MongoDB.
     *
     * @param c domain entity to serialize
     * @return MongoDB document ready for persistence
     */
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
