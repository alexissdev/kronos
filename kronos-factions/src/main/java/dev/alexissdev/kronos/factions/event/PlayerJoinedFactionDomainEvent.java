package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Evento de dominio publicado en el {@code EventBus} cuando un jugador acepta
 * una invitación y se une a una facción como nuevo miembro.
 *
 * <p>Los listeners pueden usar este evento para, por ejemplo, enviar mensajes de
 * bienvenida al jugador, actualizar estadísticas o registrar el ingreso en un log
 * de auditoría.
 *
 * <p>Este evento no cubre el caso en que el jugador fue promovido al rango de líder
 * mediante transferencia; ese flujo ocurre internamente sin publicar este evento.
 */
public final class PlayerJoinedFactionDomainEvent {

    private final UUID playerUuid;
    private final String factionId;

    /**
     * Crea el evento con los identificadores del jugador que se unió y la facción destino.
     *
     * @param playerUuid UUID del jugador que acaba de unirse a la facción
     * @param factionId  ID de la facción a la que se unió el jugador
     */
    public PlayerJoinedFactionDomainEvent(UUID playerUuid, String factionId) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
    }

    /**
     * Devuelve el UUID del jugador que se unió a la facción.
     *
     * @return UUID del jugador
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Devuelve el ID de la facción a la que se unió el jugador.
     *
     * @return ID de la facción
     */
    public String getFactionId() { return factionId; }
}
