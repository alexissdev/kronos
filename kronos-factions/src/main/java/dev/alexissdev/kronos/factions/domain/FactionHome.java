package dev.alexissdev.kronos.factions.domain;

/**
 * Teleport home location for a faction.
 *
 * <p>Stores the exact position (world, XYZ coordinates, and rotation) to which
 * members will be teleported when they use the {@code /f home} command.
 * A faction can have at most one active home; if none has been set,
 * the {@code home} field in {@link Faction} is {@code null}.
 *
 * <p>This class is immutable: once created, the location cannot change.
 * To move the home, the entire instance must be replaced via
 * {@link Faction#setHome(FactionHome)}.
 */
public final class FactionHome {

    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    /**
     * Creates a new home location for the faction.
     *
     * @param world name of the Bukkit world where the home is located
     * @param x     X coordinate (east/west)
     * @param y     Y coordinate (height)
     * @param z     Z coordinate (north/south)
     * @param yaw   horizontal rotation of the player when teleported (in degrees)
     * @param pitch vertical rotation of the player when teleported (in degrees)
     */
    public FactionHome(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    /**
     * Returns the name of the world where the home is located.
     *
     * @return Bukkit world name
     */
    public String getWorld() { return world; }

    /**
     * Returns the X coordinate of the home.
     *
     * @return X coordinate
     */
    public double getX()     { return x; }

    /**
     * Returns the Y coordinate (height) of the home.
     *
     * @return Y coordinate
     */
    public double getY()     { return y; }

    /**
     * Returns the Z coordinate of the home.
     *
     * @return Z coordinate
     */
    public double getZ()     { return z; }

    /**
     * Returns the horizontal rotation (yaw) the player will face
     * upon being teleported to the home.
     *
     * @return yaw in degrees
     */
    public float getYaw()    { return yaw; }

    /**
     * Returns the vertical rotation (pitch) the player will face
     * upon being teleported to the home.
     *
     * @return pitch in degrees
     */
    public float getPitch()  { return pitch; }
}
