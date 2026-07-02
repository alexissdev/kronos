package dev.alexissdev.kronos.factions.event;

public class FactionDtkDecrementedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final int newDtk;
    private final int maxDtk;

    public FactionDtkDecrementedDomainEvent(String factionId, String factionName, int newDtk, int maxDtk) {
        this.factionId   = factionId;
        this.factionName = factionName;
        this.newDtk      = newDtk;
        this.maxDtk      = maxDtk;
    }

    public String getFactionId()   { return factionId; }
    public String getFactionName() { return factionName; }
    public int    getNewDtk()      { return newDtk; }
    public int    getMaxDtk()      { return maxDtk; }
}
