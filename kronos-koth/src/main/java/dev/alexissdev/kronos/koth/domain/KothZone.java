package dev.alexissdev.kronos.koth.domain;

import dev.alexissdev.kronos.common.domain.CrateType;

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

    public boolean containsLocation(String world, double x, double z) {
        return this.world.equals(world)
                && x >= minX && x < maxX + 1
                && z >= minZ && z < maxZ + 1;
    }

    public boolean isInCaptureZone(String world, double x, double z) {
        return this.world.equals(world)
                && x >= captureMinX && x < captureMaxX + 1
                && z >= captureMinZ && z < captureMaxZ + 1;
    }

    public String getName()            { return name; }
    public String getWorld()           { return world; }
    public int getMinX()               { return minX; }
    public int getMinZ()               { return minZ; }
    public int getMaxX()               { return maxX; }
    public int getMaxZ()               { return maxZ; }
    public int getCaptureMinX()        { return captureMinX; }
    public int getCaptureMinZ()        { return captureMinZ; }
    public int getCaptureMaxX()        { return captureMaxX; }
    public int getCaptureMaxZ()        { return captureMaxZ; }
    public int getCaptureTimeSeconds() { return captureTimeSeconds; }
    public CrateType getRewardCrateType() { return rewardCrateType; }
    public boolean isActive()          { return active; }
    public void setActive(boolean active) { this.active = active; }
}
