package dev.alexissdev.kronos.core.domain;

public final class KothZone {

    private final String name;
    private final String world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int captureTimeSeconds;
    private final CrateType rewardCrateType;
    private boolean active;

    public KothZone(String name, String world, int minX, int minZ, int maxX, int maxZ,
                    int captureTimeSeconds, CrateType rewardCrateType) {
        this.name = name;
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.captureTimeSeconds = captureTimeSeconds;
        this.rewardCrateType = rewardCrateType;
        this.active = false;
    }

    public boolean containsLocation(String world, double x, double z) {
        return this.world.equals(world) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public String getName() { return name; }

    public String getWorld() { return world; }

    public int getMinX() { return minX; }

    public int getMinZ() { return minZ; }

    public int getMaxX() { return maxX; }

    public int getMaxZ() { return maxZ; }

    public int getCaptureTimeSeconds() { return captureTimeSeconds; }

    public CrateType getRewardCrateType() { return rewardCrateType; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }
}
