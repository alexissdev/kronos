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
 * Implementación MongoDB de {@link ClaimRepository}.
 *
 * <p>Persiste y recupera entidades {@link Claim} desde la colección {@code "claims"} de
 * MongoDB. Todas las operaciones se ejecutan en un pool de hilos independiente
 * ({@link Executors#newCachedThreadPool()}) para evitar bloquear el hilo principal
 * del servidor Bukkit.</p>
 *
 * <p>La búsqueda por chunk utiliza filtros de rango sobre los campos
 * {@code minChunkX/maxChunkX} y {@code minChunkZ/maxChunkZ}, por lo que se recomienda
 * crear índices compuestos en esos campos en producción para garantizar un rendimiento
 * adecuado.</p>
 */
@Singleton
public class MongoClaimRepository implements ClaimRepository {

    private static final String COLLECTION = "claims";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    /**
     * Construye el repositorio obteniendo la colección de claims de la base de datos.
     *
     * @param factory fábrica de conexiones MongoDB inyectada por Guice, que proporciona
     *                acceso a la base de datos configurada para el plugin
     */
    @Inject
    public MongoClaimRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ejecuta una consulta de rango en MongoDB para encontrar el claim cuyo rectángulo
     * de chunks contiene las coordenadas dadas.</p>
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
     * <p>Recupera todos los documentos cuyo campo {@code factionId} coincide con el
     * identificador recibido.</p>
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
     * <p>Itera sobre todos los documentos de la colección {@code claims} y los convierte
     * a entidades de dominio. Está pensado para la carga inicial del caché en memoria.</p>
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
     * <p>Realiza un upsert usando el campo {@code _id} del claim como clave primaria.
     * Si el documento no existe, lo inserta; si ya existe, lo reemplaza completamente.</p>
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
     * <p>Elimina en bloque todos los documentos cuyo campo {@code factionId} coincide.
     * Se invoca al disolver una facción para liberar todo su territorio.</p>
     */
    @Override
    public CompletableFuture<Void> deleteByFaction(String factionId) {
        return CompletableFuture.runAsync(
                () -> collection.deleteMany(Filters.eq("factionId", factionId)), executor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Elimina el documento cuyo campo {@code _id} coincide con el identificador dado.</p>
     */
    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", id)), executor);
    }

    /**
     * Convierte un documento BSON de MongoDB en una entidad de dominio {@link Claim}.
     *
     * <p>Si el campo {@code type} contiene un valor desconocido o nulo, se asume
     * {@link ClaimType#FACTION} para garantizar compatibilidad con datos heredados.
     * Las coordenadas de chunk nulas se normalizan a {@code 0}.</p>
     *
     * @param doc documento MongoDB a convertir
     * @return entidad {@link Claim} construida a partir del documento
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
     * Convierte una entidad de dominio {@link Claim} en un documento BSON para MongoDB.
     *
     * @param c entidad de dominio a serializar
     * @return documento MongoDB listo para persistir
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
