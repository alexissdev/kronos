package dev.alexissdev.kronos.players.domain;

import dev.alexissdev.kronos.common.domain.CrateType;

/**
 * Domain entity representing the world location of a crate (reward chest) on the server.
 *
 * <p>Crates are special chests placed at specific map coordinates that players can open
 * to receive random rewards. Each crate has a type that determines the reward pool it draws from.</p>
 *
 * <p>Crate locations are persisted in MongoDB through {@code CrateLocationRepository}
 * so that they survive server restarts.</p>
 */
public final class CrateLocation {

    private final String id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final CrateType type;

    /**
     * Creates a new crate location with all its data.
     *
     * @param id    unique identifier for this crate, generated as a random UUID
     * @param world name of the Minecraft world where the crate is located
     * @param x     X coordinate of the block where the crate resides
     * @param y     Y coordinate of the block where the crate resides
     * @param z     Z coordinate of the block where the crate resides
     * @param type  crate type that determines the available reward table
     */
    public CrateLocation(String id, String world, int x, int y, int z, CrateType type) {
        this.id    = id;
        this.world = world;
        this.x     = x;
        this.y     = y;
        this.z     = z;
        this.type  = type;
    }

    /**
     * Returns the unique identifier of this crate location.
     *
     * @return unique ID of the crate
     */
    public String    getId()    { return id; }

    /**
     * Returns the name of the Minecraft world where this crate is located.
     *
     * @return name of the server world
     */
    public String    getWorld() { return world; }

    /**
     * Returns the X coordinate of the block where this crate resides.
     *
     * @return X coordinate in the Minecraft world
     */
    public int       getX()     { return x; }

    /**
     * Returns the Y coordinate of the block where this crate resides.
     *
     * @return Y coordinate in the Minecraft world
     */
    public int       getY()     { return y; }

    /**
     * Returns the Z coordinate of the block where this crate resides.
     *
     * @return Z coordinate in the Minecraft world
     */
    public int       getZ()     { return z; }

    /**
     * Returns the crate type, which determines the category of rewards available
     * when this chest is opened.
     *
     * @return the crate type associated with this location
     */
    public CrateType getType()  { return type; }
}
