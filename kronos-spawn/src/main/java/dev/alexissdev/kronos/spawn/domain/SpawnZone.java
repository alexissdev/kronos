package dev.alexissdev.kronos.spawn.domain;

import org.bukkit.Location;

public final class SpawnZone {

    private final String world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    public SpawnZone(String world, int minX, int minZ, int maxX, int maxZ) {
        this.world = world;
        this.minX  = minX;
        this.minZ  = minZ;
        this.maxX  = maxX;
        this.maxZ  = maxZ;
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(world)) return false;
        double x = loc.getX();
        double z = loc.getZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public String getWorld() { return world; }
    public int getMinX()     { return minX; }
    public int getMinZ()     { return minZ; }
    public int getMaxX()     { return maxX; }
    public int getMaxZ()     { return maxZ; }
}
