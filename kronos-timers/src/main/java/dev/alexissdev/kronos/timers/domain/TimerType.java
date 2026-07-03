package dev.alexissdev.kronos.timers.domain;

/**
 * Enumeración de los tipos de timers disponibles en el sistema HCF.
 *
 * <p>Cada tipo representa una restricción temporal distinta aplicada sobre un jugador.
 * Los timers se persisten en Redis con TTL y se respaldan en MongoDB. El tipo de timer
 * determina qué acciones están restringidas mientras está activo y qué eventos de dominio
 * se publican al iniciarse o expirar.</p>
 */
public enum TimerType {

    /**
     * Timer de combat tag: se aplica cuando un jugador entra en combate PvP.
     * Mientras está activo impide que el jugador se desconecte del servidor sin consecuencias;
     * si cierra sesión con este timer activo, puede recibir una penalización (kill automático).
     * Duración predeterminada: 30 segundos tras el último golpe recibido o dado.
     */
    COMBAT_TAG,

    /**
     * Timer de protección PvP: se otorga a los jugadores al conectarse por primera vez
     * o al revivir tras un Deathban. Mientras está activo el jugador no puede recibir
     * ni infligir daño a otros jugadores, protegiendo a los recién conectados de emboscadas.
     */
    PVP_TIMER,

    /**
     * Cooldown de enderpearl: tiempo de espera obligatorio entre lanzamientos de enderpearl.
     * Impide el abuso de la teletransportación instantánea en combate PvP.
     */
    ENDERPEARL,

    /**
     * Cooldown de manzana dorada (Golden Apple): tiempo de espera entre el uso de manzanas
     * doradas de regeneración. Limita la curación rápida durante el combate.
     */
    GAPPLE,

    /**
     * Timer de uso del comando /home: tiempo de espera antes de poder teletransportarse
     * al hogar del jugador. Se cancela si el jugador entra en combate durante la espera.
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
