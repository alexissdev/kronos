package dev.alexissdev.kronos.timers.domain;

/**
 * Enumeration of all timer types available in the HCF system.
 *
 * <p>Each constant represents a distinct temporary restriction applied to a player.
 * Timers are persisted in Redis with a native TTL and backed up in MongoDB.
 * The timer type determines which player actions are restricted while the timer is
 * active, and which domain events are published when the timer starts or expires.</p>
 */
public enum TimerType {

    /**
     * Combat-tag timer: applied when a player enters PvP combat.
     * While active, the player cannot disconnect from the server without consequences;
     * logging out with this timer running may trigger an automatic kill penalty.
     * Default duration: 30 seconds after the last hit dealt or received.
     */
    COMBAT_TAG,

    /**
     * PvP protection timer: granted to players upon first joining the server or
     * upon reviving after a Deathban. While active, the player cannot receive or deal
     * damage to other players, shielding newcomers and revivers from ambushes.
     */
    PVP_TIMER,

    /**
     * Enderpearl cooldown: mandatory wait time between enderpearl throws.
     * Prevents abuse of instant teleportation during PvP combat.
     */
    ENDERPEARL,

    /**
     * Golden Apple (gapple) cooldown: mandatory wait time between golden apple uses.
     * Limits rapid healing during combat to keep fights balanced.
     */
    GAPPLE,

    /**
     * Home teleport timer: countdown before the player is teleported to their home
     * via the {@code /home} command. Cancelled immediately if the player enters combat
     * during the wait.
     */
    HOME,

    /**
     * Cooldown de clase de combate: tiempo de espera entre el cambio de clase activa.
     * Impide que los jugadores cambien de kit continuamente durante el combate.
     */
    CLASS_COOLDOWN,

    /**
     * Timer de logout seguro: tiempo de espera antes de que el jugador pueda desconectarse
     * del servidor de forma segura usando el comando /logout. Debe permanecer quieto
     * y fuera de combate durante toda la duración del timer.
     */
    LOGOUT,

    /**
     * Timer de comando /stuck: tiempo de espera antes de que el jugador sea teletransportado
     * a una posición segura si está atrapado. Se cancela si recibe daño durante la espera.
     */
    STUCK
}
