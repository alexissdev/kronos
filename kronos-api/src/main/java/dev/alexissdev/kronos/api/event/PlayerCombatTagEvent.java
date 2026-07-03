package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Bukkit event fired when a player receives a combat tag in the HCF system.
 * <p>
 * A combat tag is a timer that activates when a player takes damage from another player,
 * preventing actions such as disconnecting from the server without consequences. If a
 * tagged player logs out while the timer is active, the system treats it as a death
 * and applies the corresponding penalties (inventory loss, death count increment, etc.).
 * </p>
 * <p>
 * This event is cancellable: if cancelled, the combat tag will not be applied to the
 * target player. Listeners can use this to implement no-PvP zones or temporary protections
 * such as the SOTW (Start of The World) grace period.
 * </p>
 */
public class PlayerCombatTagEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID tagged;
    private final UUID tagger;
    private final long durationMillis;
    private boolean cancelled;

    /**
     * Construye un nuevo evento de combat-tag sobre un jugador.
     *
     * @param tagged         UUID del jugador que recibirá el combat-tag
     * @param tagger         UUID del jugador que provocó el combat-tag al atacar
     * @param durationMillis duración del combat-tag en milisegundos
     */
    public PlayerCombatTagEvent(UUID tagged, UUID tagger, long durationMillis) {
        this.tagged = tagged;
        this.tagger = tagger;
        this.durationMillis = durationMillis;
    }

    /**
     * Retorna el UUID del jugador que será marcado con combat-tag.
     * <p>
     * Este es el jugador que no podrá desconectarse sin consecuencias durante
     * la duración del temporizador.
     * </p>
     *
     * @return UUID del jugador objetivo del combat-tag
     */
    public UUID getTagged() { return tagged; }

    /**
     * Retorna el UUID del jugador que causó el combat-tag al atacar al objetivo.
     * <p>
     * En algunos contextos (daño por caída, lava, etc.) este valor puede coincidir
     * con el del jugador etiquetado si el sistema aplica auto-tag.
     * </p>
     *
     * @return UUID del jugador que provocó el combat-tag
     */
    public UUID getTagger() { return tagger; }

    /**
     * Retorna la duración configurada del combat-tag en milisegundos.
     * <p>
     * La duración habitual en servidores HCF es de 15–30 segundos. Si el jugador
     * recibe daño adicional antes de que expire, el temporizador se reinicia.
     * </p>
     *
     * @return duración del combat-tag en milisegundos
     */
    public long getDurationMillis() { return durationMillis; }

    /**
     * Indica si la aplicación del combat-tag ha sido cancelada por algún listener.
     *
     * @return {@code true} si el evento fue cancelado; {@code false} en caso contrario
     */
    @Override
    public boolean isCancelled() { return cancelled; }

    /**
     * Cancela o permite la aplicación del combat-tag al jugador objetivo.
     * <p>
     * Si se cancela, el jugador objetivo no recibirá el temporizador de combate
     * y podrá desconectarse libremente.
     * </p>
     *
     * @param cancelled {@code true} para cancelar el combat-tag; {@code false} para permitirlo
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
