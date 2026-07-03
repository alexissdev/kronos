package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Bukkit event fired when a player leaves or is kicked from a faction in the HCF system.
 * <p>
 * This event is not cancellable because the departure has already been processed by the
 * time it is fired. The {@link #wasKicked()} property allows listeners to distinguish
 * between a voluntary leave (the player used the leave command) and a forced removal
 * (the leader or a captain kicked them).
 * </p>
 * <p>
 * Listeners can use this event to clean up membership-related data in external plugins,
 * such as region permissions, voice channel access, or internal rankings.
 * </p>
 */
public class PlayerLeaveFactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String factionId;
    private final boolean wasKicked;

    /**
     * Construye un nuevo evento de salida de jugador de una facción.
     *
     * @param playerUuid UUID del jugador que abandonó o fue expulsado de la facción
     * @param factionId  identificador único de la facción que el jugador ha dejado
     * @param wasKicked  {@code true} si el jugador fue expulsado por el líder o un capitán;
     *                   {@code false} si abandonó la facción voluntariamente
     */
    public PlayerLeaveFactionEvent(UUID playerUuid, String factionId, boolean wasKicked) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
        this.wasKicked = wasKicked;
    }

    /**
     * Retorna el UUID del jugador que abandonó o fue expulsado de la facción.
     *
     * @return UUID del jugador que dejó la facción
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Retorna el identificador único de la facción que el jugador ha dejado.
     * <p>
     * Dado que el jugador ya no es miembro al momento de este evento, una consulta
     * a {@code FactionApi#getByPlayer(UUID)} con su UUID retornará vacío.
     * </p>
     *
     * @return ID interno de la facción que el jugador abandonó
     */
    public String getFactionId() { return factionId; }

    /**
     * Indica si la salida del jugador fue forzada por el líder o un capitán de la facción.
     *
     * @return {@code true} si el jugador fue expulsado; {@code false} si salió voluntariamente
     */
    public boolean wasKicked() { return wasKicked; }

    /**
     * Retorna la lista de handlers registrados para este evento, requerida por Bukkit.
     *
     * @return {@link HandlerList} de instancia de este evento
     */
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    /**
     * Retorna la lista global de handlers requerida por el sistema de eventos de Bukkit.
     * <p>
     * Bukkit invoca este método estático mediante reflexión para registrar listeners.
     * </p>
     *
     * @return {@link HandlerList} estática compartida de este tipo de evento
     */
    public static HandlerList getHandlerList() { return HANDLERS; }
}
