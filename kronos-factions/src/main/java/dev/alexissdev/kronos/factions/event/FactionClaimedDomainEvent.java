package dev.alexissdev.kronos.factions.event;

public final class FactionClaimedDomainEvent {

    private final String factionId;
    private final String claimId;
    private final String claimType;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    public FactionClaimedDomainEvent(String factionId, String claimId, String claimType,
                                      String world, int minChunkX, int minChunkZ,
                                      int maxChunkX, int maxChunkZ) {
        this.factionId = factionId;
        this.claimId = claimId;
        this.claimType = claimType;
        this.world = world;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
    }

    public String getFactionId() { return factionId; }
    public String getClaimId() { return claimId; }
    public String getClaimType() { return claimType; }
    public String getWorld() { return world; }
    public int getMinChunkX() { return minChunkX; }
    public int getMinChunkZ() { return minChunkZ; }
    public int getMaxChunkX() { return maxChunkX; }
    public int getMaxChunkZ() { return maxChunkZ; }
}
