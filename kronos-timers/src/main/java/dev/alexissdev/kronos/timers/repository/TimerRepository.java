package dev.alexissdev.kronos.timers.repository;

import dev.alexissdev.kronos.timers.domain.Timer;
import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato de acceso asíncrono a los timers activos de jugadores, respaldado por Redis con TTL.
 *
 * <p>Define las operaciones de lectura, escritura y eliminación sobre entidades {@link Timer}.
 * La implementación predeterminada ({@code RedisTimerRepository}) usa Redis como almacenamiento
 * primario aprovechando el TTL nativo para que los timers expiren automáticamente sin
 * necesidad de tareas programadas. Cada timer se almacena con la clave
 * {@code timer:{uuid}:{timerType}}.</p>
 *
 * <p>Todas las operaciones son no bloqueantes y devuelven {@link CompletableFuture}
 * para integrarse con el modelo asíncrono del plugin.</p>
 */
public interface TimerRepository {

    /**
     * Busca el timer activo de un jugador para el tipo de timer especificado.
     * Si el timer existe pero ya expiró según su instante de expiración, se devuelve vacío.
     *
     * @param playerUuid UUID del jugador cuyo timer se quiere consultar
     * @param type       tipo del timer a buscar
     * @return future que se resuelve con un {@link Optional} que contiene el timer activo
     *         si existe y no ha expirado, o vacío si no existe o ya expiró
     */
    CompletableFuture<Optional<Timer>> findTimer(UUID playerUuid, TimerType type);

    /**
     * Obtiene todos los timers activos de un jugador consultando cada tipo de timer existente.
     * Solo incluye en el resultado los timers que aún no han expirado.
     *
     * @param playerUuid UUID del jugador cuyos timers activos se quieren obtener
     * @return future que se resuelve con la lista de timers activos del jugador;
     *         la lista estará vacía si el jugador no tiene ningún timer activo
     */
    CompletableFuture<List<Timer>> findAllTimers(UUID playerUuid);

    /**
     * Guarda un timer en Redis con su tiempo de vida restante como TTL.
     * El TTL se calcula en segundos a partir de los milisegundos restantes del timer.
     * Redis eliminará automáticamente la clave cuando el TTL llegue a cero.
     *
     * @param timer entidad {@link Timer} con la información del timer a persistir
     * @return future que se resuelve cuando el timer ha sido almacenado en Redis
     */
    CompletableFuture<Void> saveTimer(Timer timer);

    /**
     * Elimina el timer de un jugador para el tipo especificado antes de que su TTL expire.
     * Se usa al cancelar un timer manualmente o al reiniciarlo con una nueva duración.
     *
     * @param playerUuid UUID del jugador cuyo timer se quiere eliminar
     * @param type       tipo del timer a eliminar de Redis
     * @return future que se resuelve cuando el timer ha sido eliminado
     */
    CompletableFuture<Void> deleteTimer(UUID playerUuid, TimerType type);

    /**
     * Verifica rápidamente si existe una clave en Redis para el timer del jugador indicado.
     * A diferencia de {@link #findTimer}, no deserializa el timer completo; simplemente
     * comprueba la existencia de la clave con el comando {@code EXISTS} de Redis.
     *
     * @param playerUuid UUID del jugador a verificar
     * @param type       tipo del timer cuya existencia se quiere comprobar
     * @return future que se resuelve con {@code true} si la clave existe en Redis,
     *         {@code false} si no existe o ya expiró su TTL
     */
    CompletableFuture<Boolean> hasTimer(UUID playerUuid, TimerType type);
}
