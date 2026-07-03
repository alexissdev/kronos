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

/**
 * Implementación MongoDB del repositorio de zonas KOTH.
 *
 * <p>Utiliza la colección {@code "koths"} de la base de datos MongoDB del servidor para
 * almacenar y recuperar entidades {@link KothZone}. El campo {@code _id} de cada documento
 * corresponde al nombre único del KOTH.</p>
 *
 * <p>Todas las operaciones se ejecutan de forma asíncrona en un {@code CachedThreadPool}
 * dedicado, garantizando que el hilo principal de Bukkit nunca sea bloqueado por I/O de red.</p>
 *
 * <p>Se maneja compatibilidad con documentos legados que no tienen campos de zona de captura:
 * en ese caso se usan las coordenadas del claim como fallback.</p>
 */
@Singleton
public class MongoKothRepository implements KothRepository {

    private static final String COLLECTION = "koths";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    /**
     * Constructor inyectado por Guice que obtiene la colección MongoDB a través de la fábrica.
     *
     * @param factory fábrica de conexiones MongoDB del módulo común de Kronos
     */
    @Inject
    public MongoKothRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<KothZone>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", name)).first();
            return Optional.ofNullable(doc).map(this::toZone);
        }, executor);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<List<KothZone>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<KothZone> result = new ArrayList<>();
            collection.find().forEach(doc -> result.add(toZone(doc)));
            return result;
        }, executor);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
