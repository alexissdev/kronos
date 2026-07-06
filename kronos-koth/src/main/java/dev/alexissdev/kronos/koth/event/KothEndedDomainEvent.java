package dev.alexissdev.kronos.koth.event;

/**
 * Domain event published via Guava {@code EventBus} when an active KOTH event
 * ends without having been captured by any player.
 *
 * <p>This event is distinct from {@code KothCapturedDomainEvent} in that no player
 * managed to hold the capture zone for the required time. Typical subscribers are
 * the scoreboard and timer systems, which should stop their counters and display
 * the final result to the server.</p>
 */
public final class KothEndedDomainEvent {

    private final String kothName;

    /**
     * Construye el evento con el nombre del KOTH que finalizó sin captura.
     *
     * @param kothName nombre único del KOTH cuyo evento terminó
     */
    public KothEndedDomainEvent(String kothName) {
        this.kothName = kothName;
    }

    /**
     * @return nombre único del KOTH cuyo evento ha finalizado sin captura
     */
    public String getKothName() { return kothName; }
}
