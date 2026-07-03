package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Evento de dominio publicado en el {@code EventBus} cuando un jugador abandona
 * o es expulsado de una facción.
 *
 * <p>El flag {@code wasKicked} diferencia entre las dos situaciones:
 * <ul>
 *   <li>{@code wasKicked = true}: el jugador fue expulsado por un miembro con
 *       rango suficiente (CAPTAIN o superior).</li>
 *   <li>{@code wasKicked = false}: el jugador abandonó voluntariamente la facción.</li>
 * </ul>
 *
 * <p>Tras este evento el sistema registra un cooldown de re-invitación para el jugador,
 * impidiendo que sea re-invitado inmediatamente a la misma facción.
 */
public final class PlayerLeftFactionDomainEvent {

    private final UUID playerUuid;
    private final String factionId;
    private final boolean wasKicked;

    /**
     * Crea el evento con los datos del jugador que salió de la facción.
     *
     * @param playerUuid UUID del jugador que abandonó o fue expulsado
     * @param factionId  ID de la facción que el jugador ha dejado
     * @param wasKicked  {@code true} si el jugador fue expulsado; {@code false} si se fue voluntariamente
     */
    public PlayerLeftFactionDomainEvent(UUID playerUuid, String factionId, boolean wasKicked) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
        this.wasKicked = wasKicked;
    }

    /**
     * Devuelve el UUID del jugador que salió de la facción.
     *
     * @return UUID del jugador
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Devuelve el ID de la facción que el jugador ha abandonado o de la que fue expulsado.
     *
     * @return ID de la facción
     */
    public String getFactionId() { return factionId; }

    /**
     * Indica si el jugador fue expulsado por otro miembro o si salió voluntariamente.
     *
     * @return {@code true} si fue expulsado; {@code false} si salió voluntariamente
     */
    public boolean wasKicked() { return wasKicked; }
}
