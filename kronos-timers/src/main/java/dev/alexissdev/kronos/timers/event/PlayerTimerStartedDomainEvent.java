package dev.alexissdev.kronos.timers.event;

import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.UUID;

/**
 * Evento de dominio publicado en el Guava {@code EventBus} cuando se inicia
 * un nuevo timer para un jugador.
 *
 * <p>Se publica al llamar a {@code TimerService#startTimer} y también durante la
 * carga inicial de timers en caché al conectarse el jugador (si existen timers
 * vigentes en Redis o MongoDB). Los listeners pueden reaccionar a este evento para
 * aplicar efectos visuales, mostrar mensajes al jugador o activar restricciones
 * específicas del tipo de timer iniciado.</p>
 */
public final class PlayerTimerStartedDomainEvent {

    private final UUID playerUuid;
    private final TimerType timerType;
    private final long durationMillis;

    /**
     * Crea el evento de inicio de timer con el jugador, el tipo y la duración del timer.
     *
     * @param playerUuid     UUID del jugador al que se le ha iniciado el timer
     * @param timerType      tipo del timer que acaba de iniciarse
     * @param durationMillis duración restante del timer en milisegundos desde el momento del evento
     */
    public PlayerTimerStartedDomainEvent(UUID playerUuid, TimerType timerType, long durationMillis) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
        this.durationMillis = durationMillis;
    }

    /**
     * Obtiene el UUID del jugador al que se le ha iniciado el timer.
     *
     * @return UUID del jugador afectado por el nuevo timer
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Obtiene el tipo del timer que acaba de iniciarse.
     *
     * @return tipo del timer iniciado
     */
    public TimerType getTimerType() { return timerType; }

    /**
     * Obtiene la duración restante del timer en milisegundos.
     * En timers nuevos corresponde a la duración total; en timers restaurados
     * desde Redis o MongoDB corresponde al tiempo restante en el momento de la carga.
     *
     * @return duración o tiempo restante del timer en milisegundos
     */
    public long getDurationMillis() { return durationMillis; }
}
