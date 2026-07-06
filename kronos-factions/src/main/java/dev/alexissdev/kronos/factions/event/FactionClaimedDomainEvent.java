package dev.alexissdev.kronos.factions.event;

/**
 * Domain event published on the {@code EventBus} when a faction claims a new
 * territory (claim) in the world.
 *
 * <p>Listeners of this event can, for example, update the server's visual map,
 * register the claim in the territory database, or notify faction members
 * that they have gained new land.
 *
 * <p>The claimed territory is described as a rectangle of chunks bounded by the
 * coordinates ({@code minChunkX}, {@code minChunkZ}) and
 * ({@code maxChunkX}, {@code maxChunkZ}) within the specified world.
 */
public final class FactionClaimedDomainEvent {

    private final String factionId;
    private final String claimId;
    private final String claimType;
    private final String world;
    private final int minChunkX;
    private final int minChunkZ;
    private final int maxChunkX;
    private final int maxChunkZ;

    /**
     * Creates the event with all data for the newly established claim.
     *
     * @param factionId  ID of the faction that performed the claim
     * @param claimId    unique identifier of the created claim
     * @param claimType  type of claim (e.g. {@code "FACTION"}, {@code "WARZONE"}, {@code "ROAD"})
     * @param world      name of the Bukkit world where the claim is located
     * @param minChunkX  minimum chunk X coordinate of the claim rectangle
     * @param minChunkZ  minimum chunk Z coordinate of the claim rectangle
     * @param maxChunkX  maximum chunk X coordinate of the claim rectangle
     * @param maxChunkZ  maximum chunk Z coordinate of the claim rectangle
     */
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

    /**
     * Returns the ID of the faction that performed the claim.
     *
     * @return faction ID
     */
    public String getFactionId() { return factionId; }

    /**
     * Returns the unique identifier of the newly created claim.
     *
     * @return claim ID
     */
    public String getClaimId() { return claimId; }

    /**
     * Returns the type of claim (e.g. {@code "FACTION"}, {@code "WARZONE"}).
     *
     * @return claim type
     */
    public String getClaimType() { return claimType; }

    /**
     * Returns the name of the world where the claim is located.
     *
     * @return world name
     */
    public String getWorld() { return world; }

    /**
     * Returns the minimum chunk X coordinate bounding the claim.
     *
     * @return minimum chunk X coordinate
     */
    public int getMinChunkX() { return minChunkX; }

    /**
     * Returns the minimum chunk Z coordinate bounding the claim.
     *
     * @return minimum chunk Z coordinate
     */
    public int getMinChunkZ() { return minChunkZ; }

    /**
     * Returns the maximum chunk X coordinate bounding the claim.
     *
     * @return maximum chunk X coordinate
     */
    public int getMaxChunkX() { return maxChunkX; }

    /**
     * Returns the maximum chunk Z coordinate bounding the claim.
     *
     * @return maximum chunk Z coordinate
     */
    public int getMaxChunkZ() { return maxChunkZ; }
}
