package dev.alexissdev.kronos.koth.domain;

import dev.alexissdev.kronos.common.domain.CrateType;

/**
 * Domain entity representing a KOTH (King of The Hill) zone on the HCF server.
 *
 * <p>A {@code KothZone} defines two rectangular regions on the XZ plane of the world:
 * <ul>
 *   <li><b>Claim zone</b> ({@code minX/minZ – maxX/maxZ}): the full territory of the KOTH,
 *       used for protection enforcement and map display.</li>
 *   <li><b>Capture zone</b> ({@code captureMinX/captureMinZ – captureMaxX/captureMaxZ}):
 *       the inner area where a player must remain to accumulate capture time.</li>
 * </ul>
 *
 * <p>The zone is immutable in its geographic coordinates; only the {@code active} flag
 * may change at runtime via {@link #setActive(boolean)}.</p>
 *
 * <p>When the KOTH is captured, the system delivers a crate of the {@link CrateType}
 * configured in {@code rewardCrateType} to the winning player.</p>
 */
public final class KothZone {

    private final String name;
    private final String world;

    // Full territory — used for protection and claim display
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    // Inner area where a player must stand to capture
    private final int captureMinX;
    private final int captureMinZ;
    private final int captureMaxX;
    private final int captureMaxZ;

    private final int captureTimeSeconds;
    private final CrateType rewardCrateType;
    private boolean active;

    /**
     * Constructs a new KOTH zone with all its geographic and gameplay properties.
     *
     * @param name               unique name that identifies this KOTH
     * @param world              name of the Bukkit world where the zone is located
     * @param minX               minimum X coordinate of the claim territory
     * @param minZ               minimum Z coordinate of the claim territory
     * @param maxX               maximum X coordinate of the claim territory
     * @param maxZ               maximum Z coordinate of the claim territory
     * @param captureMinX        minimum X coordinate of the inner capture zone
     * @param captureMinZ        minimum Z coordinate of the inner capture zone
     * @param captureMaxX        maximum X coordinate of the inner capture zone
     * @param captureMaxZ        maximum Z coordinate of the inner capture zone
     * @param captureTimeSeconds seconds a player must remain inside the capture zone
     *                           in order to win the event
     * @param rewardCrateType    type of crate that will be delivered to the player who captures the KOTH
     */
    public KothZone(String name, String world,
                    int minX, int minZ, int maxX, int maxZ,
                    int captureMinX, int captureMinZ, int captureMaxX, int captureMaxZ,
                    int captureTimeSeconds, CrateType rewardCrateType) {
        this.name = name;
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.captureMinX = captureMinX;
        this.captureMinZ = captureMinZ;
        this.captureMaxX = captureMaxX;
        this.captureMaxZ = captureMaxZ;
        this.captureTimeSeconds = captureTimeSeconds;
        this.rewardCrateType = rewardCrateType;
        this.active = false;
    }

    /**
     * Checks whether a given location falls within the claim territory of this zone.
     * Used to enforce build and PvP protection across the full KOTH area.
     *
     * @param world name of the world where the location resides
     * @param x     X coordinate to evaluate
     * @param z     Z coordinate to evaluate
     * @return {@code true} if the location belongs to the claim territory of this zone
     */
    public boolean containsLocation(String world, double x, double z) {
        return this.world.equals(world)
                && x >= minX && x < maxX + 1
                && z >= minZ && z < maxZ + 1;
    }

    /**
     * Checks whether a given location is inside the inner capture zone.
     * A player must remain within this area for {@link #getCaptureTimeSeconds()} seconds
     * to win the KOTH event.
     *
     * @param world name of the world where the location resides
     * @param x     X coordinate to evaluate
     * @param z     Z coordinate to evaluate
     * @return {@code true} if the location is within the capture zone
     */
    public boolean isInCaptureZone(String world, double x, double z) {
        return this.world.equals(world)
                && x >= captureMinX && x < captureMaxX + 1
                && z >= captureMinZ && z < captureMaxZ + 1;
    }

    /** @return unique name that identifies this KOTH zone */
    public String getName()            { return name; }
    /** @return name of the Bukkit world where the zone is located */
    public String getWorld()           { return world; }
    /** @return minimum X coordinate of the claim territory */
    public int getMinX()               { return minX; }
    /** @return minimum Z coordinate of the claim territory */
    public int getMinZ()               { return minZ; }
    /** @return maximum X coordinate of the claim territory */
    public int getMaxX()               { return maxX; }
    /** @return maximum Z coordinate of the claim territory */
    public int getMaxZ()               { return maxZ; }
    /** @return minimum X coordinate of the inner capture zone */
    public int getCaptureMinX()        { return captureMinX; }
    /** @return minimum Z coordinate of the inner capture zone */
    public int getCaptureMinZ()        { return captureMinZ; }
    /** @return maximum X coordinate of the inner capture zone */
    public int getCaptureMaxX()        { return captureMaxX; }
    /** @return maximum Z coordinate of the inner capture zone */
    public int getCaptureMaxZ()        { return captureMaxZ; }
    /** @return seconds a player must remain inside the capture zone to win */
    public int getCaptureTimeSeconds() { return captureTimeSeconds; }
    /** @return type of crate delivered as a reward when the KOTH is captured */
    public CrateType getRewardCrateType() { return rewardCrateType; }
    /** @return {@code true} if the KOTH event is currently running */
    public boolean isActive()          { return active; }
    /**
     * Changes the active state of the KOTH event. This method is invoked by
     * {@code KothApplicationService} during the start, end, and capture of the event.
     *
     * @param active {@code true} to mark the KOTH as active; {@code false} to deactivate it
     */
    public void setActive(boolean active) { this.active = active; }
}
