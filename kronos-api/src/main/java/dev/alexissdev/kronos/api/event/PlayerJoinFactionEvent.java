package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Bukkit event fired when a player joins a faction in the HCF system.
 * <p>
 * This event is fired both when a player accepts an invitation and when a leader or
 * captain adds them directly. It is cancellable: if a listener cancels it, the player
 * will not be added as a member of the faction.
 * </p>
 * <p>
 * External plugins can listen to this event to synchronize membership-related data,
 * such as region permissions, private chat channels, or group statistics.
 * </p>
 */
public class PlayerJoinFactionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String factionId;
    private boolean cancelled;

    /**
     * Construye un nuevo evento de ingreso de jugador a una facción.
     *
     * @param playerUuid UUID del jugador que está ingresando a la facción
     * @param factionId  identificador único de la facción a la que se une el jugador
     */
    public PlayerJoinFactionEvent(UUID playerUuid, String factionId) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
    }

    /**
     * Retorna el UUID del jugador que está ingresando a la facción.
     *
     * @return UUID del jugador que se une a la facción
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Retorna el identificador único de la facción a la que el jugador está ingresando.
     * <p>
     * Puede usarse con {@code FactionApi#getById(String)} para obtener el estado completo
     * de la facción en el momento del ingreso.
     * </p>
     *
     * @return ID interno de la facción destino
     */
    public String getFactionId() { return factionId; }

    /**
     * Indica si el ingreso del jugador a la facción ha sido cancelado por algún listener.
     *
     * @return {@code true} si el evento fue cancelado; {@code false} en caso contrario
     */
    @Override
    public boolean isCancelled() { return cancelled; }

    /**
     * Cancela o permite el ingreso del jugador a la facción.
     * <p>
     * Si se cancela, el jugador no será añadido como miembro y no se modificará
     * el estado de la facción.
     * </p>
     *
     * @param cancelled {@code true} para cancelar el ingreso; {@code false} para permitirlo
     */
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

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
