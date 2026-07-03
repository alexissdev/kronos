package dev.alexissdev.kronos.timers.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.timers.domain.Timer;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bson.Document;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repositorio de respaldo en MongoDB para los timers activos de jugadores.
 *
 * <p>Actúa como capa de durabilidad secundaria para garantizar que los timers no se
 * pierdan en caso de reinicio de Redis o fallo del servidor sin persistencia habilitada.
 * Las escrituras son de tipo "fire-and-forget" (sin esperar el resultado) para no añadir
 * latencia al flujo principal. Las lecturas solo ocurren durante el login del jugador,
 * cuando Redis no devuelve un timer activo y se consulta MongoDB como fallback.</p>
 *
 * <p>Los timers se almacenan en la colección {@code player_timers} con un ID compuesto
 * {@code {uuid}:{timerType}} como clave primaria. A diferencia de Redis, MongoDB no
 * elimina automáticamente los documentos expirados, por lo que la comprobación de
 * expiración se realiza al leer comparando el campo {@code expiresAt} con el momento actual.</p>
 */
@Singleton
public class MongoTimerBackupRepository {

    private static final String COLLECTION = "player_timers";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    /**
     * Crea el repositorio de respaldo obteniendo la colección de MongoDB
     * a través de la fábrica de conexiones.
     *
     * @param factory fábrica que provee la conexión y la base de datos de MongoDB
     */
    @Inject
    public MongoTimerBackupRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor   = Executors.newCachedThreadPool();
    }

    /**
     * Guarda o actualiza el timer en MongoDB como copia de respaldo (upsert).
     *
     * <p>La operación se ejecuta de forma asíncrona y sin bloquear al llamador.
     * El ID del documento se forma combinando el UUID del jugador con el nombre del tipo
     * de timer, garantizando unicidad por jugador y tipo.</p>
     *
     * @param timer entidad {@link Timer} a persistir como respaldo en MongoDB
     * @return future que se resuelve cuando el documento ha sido guardado en MongoDB
     */
    public CompletableFuture<Void> save(Timer timer) {
        return CompletableFuture.runAsync(() -> {
            Document doc = new Document("_id", docId(timer.getPlayerUuid(), timer.getType()))
                    .append("playerUuid", timer.getPlayerUuid().toString())
                    .append("timerType",  timer.getType().name())
                    .append("expiresAt",  timer.getExpiresAt().toEpochMilli());
            collection.replaceOne(
                    Filters.eq("_id", docId(timer.getPlayerUuid(), timer.getType())),
                    doc,
                    new ReplaceOptions().upsert(true));
        }, executor);
    }

    /**
     * Busca el timer de respaldo de un jugador en MongoDB.
     * Se usa como fallback durante el login cuando Redis no contiene el timer activo
     * (por ejemplo, tras un reinicio de Redis sin persistencia habilitada).
     *
     * <p>Si el timer existe pero su instante de expiración ya pasó, se devuelve un
     * {@link Optional} vacío para evitar restaurar timers ya expirados.</p>
     *
     * @param playerUuid UUID del jugador cuyo timer se busca en el respaldo
     * @param type       tipo del timer a buscar
     * @return future que se resuelve con un {@link Optional} que contiene el timer
     *         si existe en MongoDB y no ha expirado, o vacío si no existe o ya expiró
     */
    public CompletableFuture<Optional<Timer>> find(UUID playerUuid, TimerType type) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", docId(playerUuid, type))).first();
            if (doc == null) return Optional.empty();
            long expiresAtMs = doc.getLong("expiresAt");
            Timer timer = new Timer(playerUuid, type, Instant.ofEpochMilli(expiresAtMs));
            return timer.isExpired() ? Optional.empty() : Optional.of(timer);
        }, executor);
    }

    /**
     * Elimina el documento de respaldo de un timer de MongoDB.
     * Se llama al cancelar un timer para mantener consistencia entre Redis y MongoDB.
     *
     * @param playerUuid UUID del jugador cuyo timer de respaldo se quiere eliminar
     * @param type       tipo del timer a eliminar del respaldo
     * @return future que se resuelve cuando el documento ha sido eliminado de MongoDB
     */
    public CompletableFuture<Void> delete(UUID playerUuid, TimerType type) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", docId(playerUuid, type))), executor);
    }

    /**
     * Construye el ID compuesto del documento MongoDB para el timer de un jugador.
     * El formato es {@code {uuid}:{timerType}}, garantizando unicidad por jugador y tipo.
     *
     * @param uuid UUID del jugador propietario del timer
     * @param type tipo del timer
     * @return ID del documento en formato {@code uuid:timerType}
     */
    private static String docId(UUID uuid, TimerType type) {
        return uuid.toString() + ":" + type.name();
    }
}
