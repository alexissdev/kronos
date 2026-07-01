package dev.alexissdev.kronos.api.model;

import dev.alexissdev.kronos.claims.domain.ClaimType;

/** Immutable read-only view of a Claim for external plugin use. */
public final class ClaimSnapshot {

    private final String id;
    private final String factionId;
    private final ClaimType type;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    public ClaimSnapshot(String id, String factionId, ClaimType type, String world,
                         int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        this.id = id;
        this.factionId = factionId;
        this.type = type;
        this.world = world;
        this.minChunkX = minChunkX;
        this.minChunkZ = minChunkZ;
        this.maxChunkX = maxChunkX;
        this.maxChunkZ = maxChunkZ;
    }

    public String getId() { return id; }
    public String getFactionId() { return factionId; }
    public ClaimType getType() { return type; }
    public String getWorld() { return world; }
    public int getMinChunkX() { return minChunkX; }
    public int getMinChunkZ() { return minChunkZ; }
    public int getMaxChunkX() { return maxChunkX; }
    public int getMaxChunkZ() { return maxChunkZ; }
}
