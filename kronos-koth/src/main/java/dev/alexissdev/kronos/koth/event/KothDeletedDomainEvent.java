package dev.alexissdev.kronos.koth.event;

/**
 * Domain event published via Guava {@code EventBus} when a KOTH zone is permanently
 * deleted from the system.
 *
 * <p>Modules that maintain state tied to a KOTH (such as active timers or scoreboard
 * entries) should subscribe to this event to clean up their resources in a decoupled way.</p>
 */
public final class KothDeletedDomainEvent {

    private final String kothName;

    /**
     * Constructs the event with the name of the deleted KOTH.
     *
     * @param kothName unique name of the KOTH that was deleted
     */
    public KothDeletedDomainEvent(String kothName) {
        this.kothName = kothName;
    }

    /**
     * @return unique name of the KOTH that was removed from the system
     */
    public String getKothName() { return kothName; }
}
