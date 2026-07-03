package dev.alexissdev.kronos.api.event;

import dev.alexissdev.kronos.api.model.FactionSnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Bukkit event fired when a player creates a new faction in the HCF system.
 * <p>
 * This event is cancellable: if a listener cancels it, the faction will not be registered
 * in the system or persisted to the database. It can be used to enforce additional creation
 * restrictions such as prohibited names, per-server faction limits, or other external
 * business validations.
 * </p>
 */
public class FactionCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FactionSnapshot faction;
    private boolean cancelled;

    /**
     * Construye un nuevo evento de creación de facción.
     *
     * @param faction instantánea de solo lectura de la facción que está siendo creada,
     *                que incluye su ID, nombre, líder y estadísticas iniciales
     */
    public FactionCreateEvent(FactionSnapshot faction) {
        this.faction = faction;
    }

    /**
     * Retorna la instantánea de la facción que está siendo creada.
     * <p>
     * La instantánea refleja el estado inicial de la facción antes de ser persistida;
     * kills, deaths, DTK y balance tendrán valores por defecto (cero).
     * </p>
     *
     * @return {@link FactionSnapshot} con los datos de la nueva facción
     */
    public FactionSnapshot getFaction() { return faction; }

    /**
     * Indica si la creación de la facción ha sido cancelada por algún listener.
     *
     * @return {@code true} si el evento fue cancelado; {@code false} en caso contrario
     */
    @Override
    public boolean isCancelled() { return cancelled; }

    /**
     * Cancela o permite la creación de la facción.
     * <p>
     * Si se establece en {@code true}, la facción no será registrada en el sistema.
     * </p>
     *
     * @param cancelled {@code true} para cancelar la creación; {@code false} para permitirla
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
