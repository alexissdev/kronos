package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Bukkit event fired when a KOTH (King of The Hill) zone begins its capture period.
 * <p>
 * This event is cancellable: if a listener cancels it, the KOTH will not start its
 * countdown and the zone will not be activated for competition. It can be used to block
 * a KOTH from starting under special server conditions, such as during the SOTW
 * (Start of The World) grace period or when too few players are online.
 * </p>
 */
public class KothStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String kothName;
    private boolean cancelled;

    /**
     * Construye un nuevo evento de inicio de zona KOTH.
     *
     * @param kothName nombre identificador de la zona KOTH que está por iniciar su período de captura
     */
    public KothStartEvent(String kothName) {
        this.kothName = kothName;
    }

    /**
     * Retorna el nombre de la zona KOTH que está iniciando su período de captura.
     * <p>
     * Este nombre puede usarse para obtener más detalles de la zona a través de
     * {@code KothApi#getKoth(String)}.
     * </p>
     *
     * @return nombre de la zona KOTH que está iniciando
     */
    public String getKothName() { return kothName; }

    /**
     * Indica si el inicio del KOTH ha sido cancelado por algún listener.
     *
     * @return {@code true} si el evento fue cancelado; {@code false} en caso contrario
     */
    @Override
    public boolean isCancelled() { return cancelled; }

    /**
     * Cancela o permite el inicio del período de captura del KOTH.
     * <p>
     * Si se establece en {@code true}, la zona KOTH permanecerá inactiva
     * y no se iniciará la cuenta regresiva de captura.
     * </p>
     *
     * @param cancelled {@code true} para cancelar el inicio; {@code false} para permitirlo
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
