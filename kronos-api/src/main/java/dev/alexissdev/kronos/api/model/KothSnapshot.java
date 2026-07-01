package dev.alexissdev.kronos.api.model;

import dev.alexissdev.kronos.common.domain.CrateType;

/** Immutable read-only view of a KothZone for external plugin use. */
public final class KothSnapshot {

    private final String name;
    private final String world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final boolean active;
    private final CrateType rewardCrateType;

    public KothSnapshot(String name, String world, int minX, int minZ, int maxX, int maxZ,
                        boolean active, CrateType rewardCrateType) {
        this.name = name;
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.active = active;
        this.rewardCrateType = rewardCrateType;
    }

    public String getName() { return name; }
    public String getWorld() { return world; }
    public int getMinX() { return minX; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxZ() { return maxZ; }
    public boolean isActive() { return active; }
    public CrateType getRewardCrateType() { return rewardCrateType; }
}
