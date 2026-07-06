package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Domain event published on the {@code EventBus} when a faction is disbanded.
 *
 * <p>Disbanding can occur in two ways:
 * <ul>
 *   <li>The leader voluntarily executes the disband command.</li>
 *   <li>The faction accumulates the maximum number of administrative strikes.</li>
 * </ul>
 *
 * <p>Listeners of this event should clean up resources that depend on the faction,
 * such as releasing its claims, returning funds, or notifying members.
 */
public final class FactionDisbandedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final UUID actorUuid;

    /**
     * Creates the event with the data of the disbanded faction and the responsible actor.
     *
     * @param factionId   ID of the faction that was disbanded
     * @param factionName name of the faction at the time of disbanding
     * @param actorUuid   UUID of the player or entity that triggered the disbanding
     *                    (may be the leader or an administrator in the case of strikes)
     */
    public FactionDisbandedDomainEvent(String factionId, String factionName, UUID actorUuid) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.actorUuid = actorUuid;
    }

    /**
     * Returns the ID of the faction that was disbanded.
     *
     * @return faction ID
     */
    public String getFactionId() { return factionId; }

    /**
     * Returns the name the faction had before being disbanded.
     *
     * @return faction name
     */
    public String getFactionName() { return factionName; }

    /**
     * Returns the UUID of the player or entity that caused the disbanding.
     *
     * @return UUID of the responsible actor
     */
    public UUID getActorUuid() { return actorUuid; }
}
