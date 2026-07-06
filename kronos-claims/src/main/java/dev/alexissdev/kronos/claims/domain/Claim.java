package dev.alexissdev.kronos.claims.domain;

/**
 * Domain entity representing a claimed territory in the Minecraft world.
 *
 * <p>A {@code Claim} delimits a rectangular area of chunks that belongs to a
 * faction or to a special server zone (spawn, warzone, koth, etc.). Position
 * is expressed in chunk coordinates rather than block coordinates to simplify
 * ownership lookups during player movement.</p>
 *
 * <p>This class is immutable: once created, its attributes do not change. To modify
 * the owner or boundaries of a territory, the existing claim must be deleted and a
 * new one created.</p>
 */
public final class Claim {

    private final String id;
    private final String factionId;
    private final ClaimType type;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    /**
     * Constructs a new claim with all its attributes.
     *
     * @param id        unique identifier of the claim (UUID as a String)
     * @param factionId identifier of the owning faction, or {@code null} for system zones
     * @param type      territory category (see {@link ClaimType})
     * @param world     name of the Minecraft world where the claim is located
     * @param minChunkX minimum X chunk coordinate bounding the rectangle
     * @param minChunkZ minimum Z chunk coordinate bounding the rectangle
     * @param maxChunkX maximum X chunk coordinate bounding the rectangle
     * @param maxChunkZ maximum Z chunk coordinate bounding the rectangle
     */
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

    /**
     * Checks whether the specified chunk falls within the bounds of this claim.
     *
     * <p>Used primarily in the movement listener to determine which territory
     * a player is currently standing in.</p>
     *
     * @param chunkX X coordinate of the chunk to evaluate
     * @param chunkZ Z coordinate of the chunk to evaluate
     * @return {@code true} if the chunk is contained within the claim's rectangle
     */
    public boolean containsChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX && chunkX <= maxChunkX
                && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    /**
     * Calculates the total number of chunks covered by this claim.
     *
     * <p>Used to validate size limits when claiming territory or to display
     * faction statistics.</p>
     *
     * @return number of chunks in the claim's rectangular area
     */
    public int getChunkCount() {
        return (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
    }

    /**
     * Returns the unique identifier of this claim.
     *
     * @return UUID as a String assigned to the claim
     */
    public String getId() { return id; }

    /**
     * Returns the identifier of the faction that owns this territory.
     *
     * @return faction ID, or {@code null} if this is a system-owned claim
     */
    public String getFactionId() { return factionId; }

    /**
     * Returns the category of this territory.
     *
     * @return claim type as defined by {@link ClaimType}
     */
    public ClaimType getType() { return type; }

    /**
     * Returns the name of the Minecraft world where this claim is located.
     *
     * @return world name (for example, {@code "world"})
     */
    public String getWorld() { return world; }

    /**
     * Returns the minimum X chunk coordinate bounding this claim.
     *
     * @return western boundary of the territory in chunk coordinates
     */
    public int getMinChunkX() { return minChunkX; }

    /**
     * Returns the minimum Z chunk coordinate bounding this claim.
     *
     * @return northern boundary of the territory in chunk coordinates
     */
    public int getMinChunkZ() { return minChunkZ; }

    /**
     * Returns the maximum X chunk coordinate bounding this claim.
     *
     * @return eastern boundary of the territory in chunk coordinates
     */
    public int getMaxChunkX() { return maxChunkX; }

    /**
     * Returns the maximum Z chunk coordinate bounding this claim.
     *
     * @return southern boundary of the territory in chunk coordinates
     */
    public int getMaxChunkZ() { return maxChunkZ; }
}
