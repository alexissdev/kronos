package dev.alexissdev.kronos.timers.event;

import java.util.UUID;

/**
 * Evento de dominio publicado en el Guava {@code EventBus} cuando un jugador
 * es marcado en combate (combat tag) al recibir o infligir daño a otro jugador.
 *
 * <p>Al publicarse este evento, ambos jugadores involucrados en el combate reciben
 * un timer de tipo {@link dev.alexissdev.kronos.timers.domain.TimerType#COMBAT_TAG}
 * que les impide desconectarse del servidor sin consecuencias durante su duración.
 * Los listeners suscritos al {@code EventBus} pueden reaccionar a este evento para
 * mostrar notificaciones o aplicar lógica adicional de combate.</p>
 */
public final class PlayerCombatTaggedDomainEvent {

    private final UUID taggedUuid;
    private final UUID taggerUuid;
    private final long durationMillis;

    /**
     * Crea el evento de combat tag con los jugadores involucrados y la duración del timer.
     *
     * @param taggedUuid    UUID del jugador que fue marcado en combate (recibió el tag)
     * @param taggerUuid    UUID del jugador que inició el combate y provocó el tag
     * @param durationMillis duración del timer de combat tag en milisegundos
     */
    public PlayerCombatTaggedDomainEvent(UUID taggedUuid, UUID taggerUuid, long durationMillis) {
        this.taggedUuid = taggedUuid;
        this.taggerUuid = taggerUuid;
        this.durationMillis = durationMillis;
    }

    /**
     * Obtiene el UUID del jugador que fue marcado en combate y al que se le aplicó el timer.
     *
     * @return UUID del jugador marcado en combate
     */
    public UUID getTaggedUuid() { return taggedUuid; }

    /**
     * Obtiene el UUID del jugador que inició el combate y provocó el marcado.
     *
     * @return UUID del jugador que realizó el ataque o inició el combate
     */
    public UUID getTaggerUuid() { return taggerUuid; }

    /**
     * Obtiene la duración del timer de combat tag aplicado a ambos jugadores.
     *
     * @return duración del combat tag en milisegundos (típicamente 30 000 ms)
     */
    public long getDurationMillis() { return durationMillis; }
}
