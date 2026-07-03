package dev.alexissdev.kronos.timers.event;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.UUID;

/**
 * Evento de dominio publicado en el Guava {@code EventBus} cuando el timer
 * de un jugador expira o es cancelado manualmente.
 *
 * <p>Se publica tanto cuando un timer llega a su tiempo de expiración natural como
 * cuando es cancelado explícitamente mediante {@code TimerService#cancelTimer}.
 * Los listeners pueden reaccionar a este evento para liberar restricciones al jugador,
 * notificarle del fin del timer o ejecutar efectos secundarios relacionados con el tipo
 * de timer que expiró (por ejemplo, permitir la desconexión tras el combat tag).</p>
 */
public final class PlayerTimerExpiredDomainEvent {

    private final UUID playerUuid;
    private final TimerType timerType;

    /**
     * Crea el evento de expiración de timer con el jugador afectado y el tipo de timer.
     *
     * @param playerUuid UUID del jugador cuyo timer ha expirado o fue cancelado
     * @param timerType  tipo del timer que ha finalizado
     */
    public PlayerTimerExpiredDomainEvent(UUID playerUuid, TimerType timerType) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
    }

    /**
     * Obtiene el UUID del jugador cuyo timer ha expirado.
     *
     * @return UUID del jugador afectado por la expiración del timer
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Obtiene el tipo del timer que ha expirado, permitiendo a los listeners
     * filtrar y reaccionar únicamente al tipo de timer que les interesa.
     *
     * @return tipo del timer que ha finalizado
     */
    public TimerType getTimerType() { return timerType; }
}
