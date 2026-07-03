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

/**
 * Implementación de {@link PlayerRepository} que persiste perfiles de jugadores
 * en la colección {@code players} de MongoDB.
 *
 * <p>Cada jugador se almacena como un documento BSON usando su UUID como clave primaria
 * ({@code _id}). Las operaciones de lectura y escritura se ejecutan en un pool de hilos
 * dedicado para evitar bloquear el hilo principal del servidor Bukkit.</p>
 *
 * <p>La operación de guardado utiliza {@code upsert} para insertar o reemplazar el
 * documento completo, garantizando consistencia sin necesidad de operaciones parciales.</p>
 */
@Singleton
public class MongoPlayerRepository implements PlayerRepository {

    private static final String COLLECTION = "players";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    /**
     * Crea el repositorio obteniendo la colección de MongoDB a través de la fábrica de conexiones.
     * Inicializa un pool de hilos en caché para ejecutar las operaciones de forma asíncrona.
     *
     * @param factory fábrica que provee la conexión y la base de datos de MongoDB
     */
    @Inject
    public MongoPlayerRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Busca el documento en MongoDB usando el UUID como filtro sobre el campo {@code _id}.
     * La operación se ejecuta de forma asíncrona en el pool de hilos del repositorio.</p>
     */
    @Override
    public CompletableFuture<Optional<HCFPlayer>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", uuid.toString())).first();
            return Optional.ofNullable(doc).map(this::toPlayer);
        }, executor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Realiza un upsert del documento completo del jugador. Si ya existe un documento
     * con el mismo {@code _id}, es reemplazado en su totalidad; si no existe, se inserta.</p>
     */
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

    /**
     * Convierte un documento BSON de MongoDB en una entidad {@link HCFPlayer}.
     * Si el campo {@code activeKit} contiene un valor desconocido, se usa {@link KitType#DIAMOND}
     * como valor de respaldo para evitar errores de deserialización.
     *
     * @param doc documento BSON obtenido de la colección de MongoDB
     * @return entidad {@link HCFPlayer} con los datos del documento
     */
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

    /**
     * Convierte una entidad {@link HCFPlayer} en un documento BSON apto para MongoDB.
     * El UUID se serializa como {@code String} en el campo {@code _id}.
     *
     * @param p entidad {@link HCFPlayer} a serializar
     * @return documento BSON con todos los campos del jugador
     */
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
