package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Domain event published on the {@code EventBus} when a new faction is created.
 *
 * <p>Listeners of this event can use it to, among other things, broadcast global
 * announcements on the server, record faction growth metrics, or initialise
 * auxiliary resources associated with the newly created faction.
 */
public final class FactionCreatedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final UUID leaderId;

    /**
     * Creates the event with the data of the newly founded faction.
     *
     * @param factionId   unique ID assigned to the new faction
     * @param factionName visible name chosen by the founder
     * @param leaderId    UUID of the player who founded and leads the faction
     */
    public FactionCreatedDomainEvent(String factionId, String factionName, UUID leaderId) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.leaderId = leaderId;
    }

    /**
     * Returns the unique ID of the created faction.
     *
     * @return faction ID
     */
    public String getFactionId() { return factionId; }

    /**
     * Returns the name of the created faction.
     *
     * @return faction name
     */
    public String getFactionName() { return factionName; }

    /**
     * Returns the UUID of the player who created and leads the faction.
     *
     * @return UUID of the founding leader
     */
    public UUID getLeaderId() { return leaderId; }
}
