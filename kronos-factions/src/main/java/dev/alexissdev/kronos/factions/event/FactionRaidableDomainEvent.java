package dev.alexissdev.kronos.factions.event;

/**
 * Domain event published on the {@code EventBus} when a faction becomes
 * <em>raidable</em>, that is, when its DTK counter reaches 0.
 *
 * <p>A raidable faction has its claims exposed: enemy factions can overclaim
 * its territory without restrictions. This event marks the beginning of an
 * official raid against the affected faction.
 *
 * <p>Typical listeners of this event include:
 * <ul>
 *   <li>Server-wide announcements indicating that the faction can be raided.</li>
 *   <li>Updates to visual indicators on the server map.</li>
 *   <li>Push notifications to the members of the faction under threat.</li>
 * </ul>
 */
public class FactionRaidableDomainEvent {

    private final String factionId;
    private final String factionName;

    /**
     * Creates the event with the identifying data of the raidable faction.
     *
     * @param factionId   ID of the faction that has become raidable
     * @param factionName visible name of the affected faction
     */
    public FactionRaidableDomainEvent(String factionId, String factionName) {
        this.factionId   = factionId;
        this.factionName = factionName;
    }

    /**
     * Returns the ID of the faction that has become raidable.
     *
     * @return faction ID
     */
    public String getFactionId()   { return factionId; }

    /**
     * Returns the name of the faction that has become raidable.
     *
     * @return faction name
     */
    public String getFactionName() { return factionName; }
}
