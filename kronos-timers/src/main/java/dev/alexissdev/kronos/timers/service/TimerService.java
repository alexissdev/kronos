package dev.alexissdev.kronos.timers.service;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaz de servicio de dominio para la gestión del ciclo de vida de timers en el sistema HCF.
 *
 * <p>Define las operaciones fundamentales para iniciar, cancelar y consultar timers de jugadores.
 * Cada timer representa una restricción temporal (combat tag, PvP timer, cooldowns, etc.)
 * que condiciona las acciones disponibles para el jugador mientras está activo.</p>
 *
 * <p>El parámetro de tipo genérico {@code T} representa el identificador del sujeto del timer
 * (generalmente {@link java.util.UUID}). La implementación principal es
 * {@code TimerApplicationService}.</p>
 *
 * @param <T> tipo del identificador del sujeto del timer
 */
public interface TimerService<T> {

    /**
     * Inicia un nuevo timer para el jugador con el tipo y duración especificados.
     *
     * <p>Persiste el timer en Redis con TTL, actualiza la caché en memoria, y publica
     * un {@code PlayerTimerStartedDomainEvent} en el {@code EventBus} para notificar
     * a los listeners interesados.</p>
     *
     * @param playerUuid     UUID del jugador al que se le inicia el timer
     * @param type           tipo del timer que determina las restricciones a aplicar
     * @param durationMillis duración del timer en milisegundos desde el momento de inicio
     * @return future que se resuelve cuando el timer ha sido persistido en Redis
     */
    CompletableFuture<Void> startTimer(UUID playerUuid, TimerType type, long durationMillis);

    /**
     * Cancela y elimina el timer activo de un jugador para el tipo especificado.
     *
     * <p>Elimina el timer de Redis, lo marca como inactivo en la caché en memoria y
     * publica un {@code PlayerTimerExpiredDomainEvent} si el timer estaba activo en caché.</p>
     *
     * @param playerUuid UUID del jugador cuyo timer se quiere cancelar
     * @param type       tipo del timer a cancelar
     * @return future que se resuelve cuando el timer ha sido eliminado de Redis
     */
    CompletableFuture<Void> cancelTimer(UUID playerUuid, TimerType type);

    /**
     * Verifica de forma asíncrona si un jugador tiene actualmente activo el timer del
     * tipo especificado, consultando el estado real en Redis.
     *
     * <p>Actualiza la caché en memoria con el resultado de la consulta. Si el timer
     * estaba en caché pero ya expiró en Redis, publica un {@code PlayerTimerExpiredDomainEvent}.</p>
     *
     * @param playerUuid UUID del jugador a verificar
     * @param type       tipo del timer cuya actividad se quiere comprobar
     * @return future que se resuelve con {@code true} si el timer está activo y no ha expirado,
     *         {@code false} si el timer no existe o ya expiró
     */
    CompletableFuture<Boolean> hasActiveTimer(UUID playerUuid, TimerType type);

    /**
     * Obtiene de forma asíncrona el tiempo restante en milisegundos del timer activo de un jugador.
     *
     * @param playerUuid UUID del jugador cuyo tiempo restante se quiere consultar
     * @param type       tipo del timer a consultar
     * @return future que se resuelve con un {@link OptionalLong} que contiene los milisegundos
     *         restantes si el timer está activo, o vacío si el timer no existe o ya expiró
     */
    CompletableFuture<OptionalLong> getRemainingMillis(UUID playerUuid, TimerType type);
}
