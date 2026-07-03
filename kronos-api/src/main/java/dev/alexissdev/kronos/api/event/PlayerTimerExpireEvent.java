package dev.alexissdev.kronos.api.event;

import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Bukkit event fired when an active player timer expires in the HCF system.
 * <p>
 * HCF timers include the combat tag, the new-player PvP timer, and the enderpearl
 * cooldown, among others. This event notifies listeners when any of them reaches zero
 * naturally (i.e., without manual cancellation).
 * </p>
 * <p>
 * This event is not cancellable because the timer has already finished by the time it
 * fires. Listeners can use it to trigger visual effects, play sounds, or update user
 * interfaces when the player exits an active timer state.
 * </p>
 */
public class PlayerTimerExpireEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final TimerType timerType;

    /**
     * Construye un nuevo evento de expiración de temporizador.
     *
     * @param playerUuid UUID del jugador cuyo temporizador acaba de expirar
     * @param timerType  tipo de temporizador que expiró (por ejemplo, {@code COMBAT_TAG},
     *                   {@code PVP_TIMER} o {@code ENDERPEARL})
     */
    public PlayerTimerExpireEvent(UUID playerUuid, TimerType timerType) {
        this.playerUuid = playerUuid;
        this.timerType = timerType;
    }

    /**
     * Retorna el UUID del jugador cuyo temporizador acaba de expirar.
     *
     * @return UUID del jugador afectado
     */
    public UUID getPlayerUuid() { return playerUuid; }

    /**
     * Retorna el tipo de temporizador que expiró.
     * <p>
     * Permite a los listeners reaccionar de forma específica según el tipo:
     * por ejemplo, al expirar el combat-tag se puede permitir al jugador
     * ejecutar comandos que estaban bloqueados durante el combate.
     * </p>
     *
     * @return {@link TimerType} que identifica qué tipo de temporizador finalizó
     */
    public TimerType getTimerType() { return timerType; }

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
