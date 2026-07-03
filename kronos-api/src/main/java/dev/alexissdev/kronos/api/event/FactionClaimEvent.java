package dev.alexissdev.kronos.api.event;

import dev.alexissdev.kronos.api.model.ClaimSnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Bukkit event fired when a faction attempts to claim a territory (chunk).
 * <p>
 * This event is cancellable: if a listener calls {@link #setCancelled(boolean) setCancelled(true)}
 * before the claiming process completes, the chunk will not be assigned to the faction.
 * It can be used to enforce additional claiming restrictions, such as per-faction territory
 * limits or protected zones managed by other plugins.
 * </p>
 */
public class FactionClaimEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String factionId;
    private final ClaimSnapshot claim;
    private boolean cancelled;

    /**
     * Construye un nuevo evento de reclamación de territorio.
     *
     * @param factionId identificador único de la facción que realiza la reclamación
     * @param claim     instantánea de solo lectura del chunk que será reclamado,
     *                  incluyendo coordenadas y tipo de zona
     */
    public FactionClaimEvent(String factionId, ClaimSnapshot claim) {
        this.factionId = factionId;
        this.claim = claim;
    }

    /**
     * Retorna el identificador único de la facción que está intentando reclamar el territorio.
     *
     * @return ID interno de la facción reclamante
     */
    public String getFactionId() { return factionId; }

    /**
     * Retorna la instantánea del chunk que está siendo reclamado.
     * <p>
     * La instantánea incluye el tipo de zona, las coordenadas de chunk mínimas y máximas,
     * y el mundo donde se ubica el territorio.
     * </p>
     *
     * @return {@link ClaimSnapshot} con los datos del territorio en disputa
     */
    public ClaimSnapshot getClaim() { return claim; }

    /**
     * Indica si la reclamación del territorio ha sido cancelada por algún listener.
     *
     * @return {@code true} si el evento fue cancelado; {@code false} en caso contrario
     */
    @Override
    public boolean isCancelled() { return cancelled; }

    /**
     * Cancela o permite la reclamación del territorio.
     * <p>
     * Si se establece en {@code true}, la facción no obtendrá la propiedad del chunk.
     * </p>
     *
     * @param cancelled {@code true} para cancelar la reclamación; {@code false} para permitirla
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
