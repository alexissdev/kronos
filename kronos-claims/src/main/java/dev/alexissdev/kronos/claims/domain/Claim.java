package dev.alexissdev.kronos.claims.domain;

public final class Claim {

    private final String id;
    private final String factionId;
    private final ClaimType type;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    public Claim(String id, String factionId, ClaimType type, String world,
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

    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX && chunkX <= maxChunkX
                && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    public int getChunkCount() {
        return (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
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
