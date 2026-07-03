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

/**
 * Implementación de {@link CrateLocationRepository} que persiste las ubicaciones
 * de crates en la colección {@code crate_locations} de MongoDB.
 *
 * <p>Cada crate se almacena como un documento BSON con su ID como clave primaria,
 * junto a las coordenadas del mundo y el tipo. Las búsquedas por coordenadas
 * combinan filtros sobre los campos {@code world}, {@code x}, {@code y} y {@code z}.</p>
 *
 * <p>Todas las operaciones se ejecutan de forma asíncrona en un pool de hilos dedicado
 * para no bloquear el hilo principal del servidor Bukkit.</p>
 */
@Singleton
public class MongoCrateLocationRepository implements CrateLocationRepository {

    private static final String COLLECTION = "crate_locations";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    /**
     * Crea el repositorio obteniendo la colección de MongoDB a través de la fábrica de conexiones.
     *
     * @param factory fábrica que provee la conexión y la base de datos de MongoDB
     */
    @Inject
    public MongoCrateLocationRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Aplica un filtro compuesto sobre los cuatro campos de coordenadas
     * ({@code world}, {@code x}, {@code y}, {@code z}) para localizar el crate.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Recorre todos los documentos de la colección y los convierte en entidades
     * {@link CrateLocation}. Se usa al iniciar el servidor para pre-cargar los crates.</p>
     */
    @Override
    public CompletableFuture<List<CrateLocation>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<CrateLocation> result = new ArrayList<>();
            collection.find().forEach(doc -> result.add(toLocation(doc)));
            return result;
        }, executor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Realiza un upsert usando el ID del crate como clave primaria ({@code _id}).
     * Si ya existe un crate con ese ID, es reemplazado; si no, se inserta.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Elimina el documento de la colección usando el ID como filtro.</p>
     */
    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", id)), executor);
    }

    /**
     * Convierte un documento BSON de MongoDB en una entidad {@link CrateLocation}.
     * Si el campo {@code type} contiene un valor desconocido, se usa {@link CrateType#KOTH}
     * como valor de respaldo.
     *
     * @param doc documento BSON de la colección {@code crate_locations}
     * @return entidad {@link CrateLocation} con los datos del documento
     */
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

    /**
     * Convierte una entidad {@link CrateLocation} en un documento BSON apto para MongoDB.
     *
     * @param loc entidad {@link CrateLocation} a serializar
     * @return documento BSON con todos los campos de la ubicación del crate
     */
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
