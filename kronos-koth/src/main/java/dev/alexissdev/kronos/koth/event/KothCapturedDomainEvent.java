package dev.alexissdev.kronos.koth.event;

import java.util.UUID;

/**
 * Domain event published via Guava {@code EventBus} when a player successfully captures
 * a KOTH event by remaining in the capture zone for the required amount of time.
 *
 * <p>Subscribers of this event (e.g. the reward system) can read the captor's UUID
 * to deliver the crate matching the reward type configured in the {@code KothZone}.</p>
 */
public final class KothCapturedDomainEvent {

    private final String kothName;
    private final UUID captorUuid;

    /**
     * Constructs the event with the data of the captured KOTH and the captor player.
     *
     * @param kothName    unique name of the KOTH that was captured
     * @param captorUuid  UUID of the player who performed the capture
     */
    public KothCapturedDomainEvent(String kothName, UUID captorUuid) {
        this.kothName = kothName;
        this.captorUuid = captorUuid;
    }

    /**
     * @return unique name of the KOTH that was captured
     */
    public String getKothName() { return kothName; }

    /**
     * @return UUID of the player who captured the KOTH and should receive the reward
     */
    public UUID getCaptorUuid() { return captorUuid; }
}
