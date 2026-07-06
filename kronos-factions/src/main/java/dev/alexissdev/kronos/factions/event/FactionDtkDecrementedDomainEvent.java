package dev.alexissdev.kronos.factions.event;

/**
 * Domain event published on the {@code EventBus} each time a faction's
 * DTK (Deaths To Kick) counter decreases as a result of one of its members dying.
 *
 * <p>This event is generated <strong>every</strong> time a member dies and there are
 * still DTK remaining to consume, so it can be used to send progressive alerts
 * to faction members (e.g. "Warning! Only 5 DTK left").
 *
 * <p>If after the decrement the DTK reaches 0, the system will also publish
 * {@link FactionRaidableDomainEvent} within the same transaction.
 */
public class FactionDtkDecrementedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final int newDtk;
    private final int maxDtk;

    /**
     * Creates the event with the updated DTK state of the faction.
     *
     * @param factionId   ID of the faction whose DTK was decremented
     * @param factionName name of the faction
     * @param newDtk      remaining DTK value after the decrement
     * @param maxDtk      maximum DTK value configured for the faction
     */
    public FactionDtkDecrementedDomainEvent(String factionId, String factionName, int newDtk, int maxDtk) {
        this.factionId   = factionId;
        this.factionName = factionName;
        this.newDtk      = newDtk;
        this.maxDtk      = maxDtk;
    }

    /**
     * Returns the ID of the affected faction.
     *
     * @return faction ID
     */
    public String getFactionId()   { return factionId; }

    /**
     * Returns the name of the affected faction.
     *
     * @return faction name
     */
    public String getFactionName() { return factionName; }

    /**
     * Returns the new remaining DTK value after the decrement.
     *
     * <p>A value of 0 indicates that the faction has entered the raidable state.
     *
     * @return updated remaining DTK
     */
    public int    getNewDtk()      { return newDtk; }

    /**
     * Returns the maximum DTK configured for this faction.
     *
     * <p>Useful for calculating the remaining DTK percentage and displaying progress bars.
     *
     * @return maximum DTK
     */
    public int    getMaxDtk()      { return maxDtk; }
}
