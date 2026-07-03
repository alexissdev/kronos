package dev.alexissdev.kronos.plugin.chat;

/**
 * Modos de chat disponibles para los jugadores en el sistema HCF.
 *
 * <p>Cada modo determina el alcance de los mensajes que envía un jugador:
 * <ul>
 *   <li>{@link #GLOBAL} — el mensaje llega a todos los jugadores conectados.</li>
 *   <li>{@link #FACTION} — el mensaje solo lo reciben los miembros de la facción del remitente.</li>
 *   <li>{@link #ALLY} — el mensaje llega a los miembros de la facción del remitente y a los de
 *       todas sus facciones aliadas.</li>
 * </ul>
 * El modo activo de cada jugador es gestionado por {@link ChatManager}.
 */
public enum ChatMode {

    /** Modo de chat público; los mensajes son visibles para todos los jugadores en línea. */
    GLOBAL,

    /** Modo de chat de facción; los mensajes solo son visibles para los miembros de la propia facción. */
    FACTION,

    /** Modo de chat de aliados; los mensajes llegan a los miembros de la facción propia y de sus aliadas. */
    ALLY
}
