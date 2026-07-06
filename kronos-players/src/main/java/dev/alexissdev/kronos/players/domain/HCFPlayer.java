package dev.alexissdev.kronos.players.domain;

import java.util.UUID;

/**
 * Domain entity representing a player's persistent profile within the HCF system.
 *
 * <p>Stores all persistent data for a player: combat statistics (kills and deaths),
 * the number of available lives, the currently selected active kit, the saved inventory
 * serialized as JSON, and the PvP protection timer state. This entity is persisted in
 * MongoDB through {@code PlayerRepository}.</p>
 *
 * <p>In the HCF system, lives are a limited resource: when a player dies while the
 * Deathban timer is active they lose one life. Upon reaching zero lives the player is
 * temporarily banned from the server (Deathban). Lives regenerate automatically over time.</p>
 */
public final class HCFPlayer {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int lives;
    private boolean pvpTimerGiven;
    private KitType activeKit;
    private String savedInventoryJson;
    private long lastLifeRegenAt;

    /**
     * Creates a new player profile with default values for the very first time the
     * player connects to the server.
     *
     * <p>Initial values are: 0 kills, 0 deaths, 3 lives, no PvP timer granted,
     * and {@link KitType#DIAMOND} as the active kit.</p>
     *
     * @param uuid unique Minecraft UUID of the player
     * @param name username of the player at the time of registration
     */
    public HCFPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
        this.lives = 3;
        this.pvpTimerGiven = false;
        this.activeKit = KitType.DIAMOND;
        this.lastLifeRegenAt = System.currentTimeMillis();
    }

    /**
     * Reconstructs a full player profile with all its data as loaded from the database.
     * This constructor is used exclusively by the persistence layer when deserializing
     * a MongoDB document back into a domain entity.
     *
     * @param uuid               unique UUID of the player
     * @param name               current username of the player
     * @param kills              total number of registered kills
     * @param deaths             total number of registered deaths
     * @param activeKit          the active kit selected by the player
     * @param savedInventoryJson saved inventory serialized as JSON; may be {@code null}
     * @param lives              number of lives remaining for the player
     * @param pvpTimerGiven      {@code true} if the PvP protection timer was already granted on login
     * @param lastLifeRegenAt    Unix timestamp in milliseconds of the last life regeneration
     */
    public HCFPlayer(UUID uuid, String name, int kills, int deaths,
                     KitType activeKit, String savedInventoryJson, int lives,
                     boolean pvpTimerGiven, long lastLifeRegenAt) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.activeKit = activeKit;
        this.savedInventoryJson = savedInventoryJson;
        this.lives = lives;
        this.pvpTimerGiven = pvpTimerGiven;
        this.lastLifeRegenAt = lastLifeRegenAt;
    }

    /**
     * Increments the player's kill counter by one.
     * Called when this player kills another player in PvP combat.
     */
    public void incrementKills() { kills++; }

    /**
     * Increments the player's death counter by one.
     * Called when this player is killed by another player in PvP combat.
     */
    public void incrementDeaths() { deaths++; }

    /**
     * Decrements the player's life count by one, without going below zero.
     * Called when the player dies while the Deathban timer is active.
     * If the result reaches zero, the player will be subject to Deathban
     * on the next verification cycle.
     *
     * @return the number of remaining lives after the decrement (minimum 0)
     */
    public int decrementLives() {
        if (lives > 0) lives--;
        return lives;
    }

    /**
     * Attempts to regenerate one life for the player if all required conditions are met.
     *
     * <p>Regeneration occurs only if: (1) the player has fewer lives than the allowed
     * maximum, and (2) the minimum time interval has elapsed since the last regeneration.</p>
     *
     * @param maxLives        maximum number of lives the player is allowed to have
     * @param regenIntervalMs minimum time in milliseconds that must elapse between regenerations
     * @return {@code true} if a life was successfully regenerated, {@code false} if conditions were not met
     */
    public boolean tryRegenLife(int maxLives, long regenIntervalMs) {
        if (lives >= maxLives) return false;
        if (System.currentTimeMillis() - lastLifeRegenAt < regenIntervalMs) return false;
        lives++;
        lastLifeRegenAt = System.currentTimeMillis();
        return true;
    }

    /**
     * Directly sets the player's life count.
     * Used to restore lives after a Deathban has expired.
     *
     * @param lives new life count to assign to the player
     */
    public void setLives(int lives) { this.lives = lives; }

    /**
     * Returns the Unix timestamp in milliseconds of the last time this player regenerated a life.
     *
     * @return Unix timestamp in milliseconds of the last life regeneration
     */
    public long getLastLifeRegenAt()          { return lastLifeRegenAt; }

    /**
     * Updates the timestamp of the last life regeneration.
     *
     * @param t new Unix timestamp in milliseconds
     */
    public void setLastLifeRegenAt(long t)    { this.lastLifeRegenAt = t; }

    /**
     * Returns whether the PvP protection timer has already been granted to the player on login.
     * The PvP timer shields newly connected players from taking damage for a set duration.
     *
     * @return {@code true} if the PvP protection timer has already been granted in the current session
     */
    public boolean isPvpTimerGiven()          { return pvpTimerGiven; }

    /**
     * Sets the state of the player's PvP protection timer.
     *
     * @param v {@code true} to mark the timer as already granted, {@code false} to reset it
     */
    public void setPvpTimerGiven(boolean v)   { this.pvpTimerGiven = v; }

    /**
     * Returns the player's unique Minecraft UUID.
     *
     * @return the player's UUID
     */
    public UUID   getUuid()   { return uuid; }

    /**
     * Returns the player's current username.
     *
     * @return the player's username
     */
    public String getName()   { return name; }

    /**
     * Updates the player's username.
     * Called when the player reconnects with a different name than the one on record.
     *
     * @param name new username of the player
     */
    public void   setName(String name) { this.name = name; }

    /**
     * Returns the total number of kills registered for this player on the server.
     *
     * @return total kill count
     */
    public int  getKills()  { return kills; }

    /**
     * Returns the total number of deaths registered for this player on the server.
     *
     * @return total death count
     */
    public int  getDeaths() { return deaths; }

    /**
     * Returns the player's remaining life count.
     * A value of zero indicates that the player is subject to the Deathban system.
     *
     * @return number of remaining lives
     */
    public int  getLives()  { return lives; }

    /**
     * Returns the kit type currently active for this player.
     *
     * @return the player's active kit type
     */
    public KitType getActiveKit()                { return activeKit; }

    /**
     * Sets the player's active kit.
     *
     * @param activeKit new kit type to activate
     */
    public void    setActiveKit(KitType activeKit) { this.activeKit = activeKit; }

    /**
     * Returns the player's inventory serialized as a JSON string.
     * Used to save and restore the player's inventory across sessions.
     *
     * @return JSON string of the saved inventory, or {@code null} if none has been saved yet
     */
    public String getSavedInventoryJson() { return savedInventoryJson; }

    /**
     * Sets the player's inventory as a serialized JSON string.
     *
     * @param savedInventoryJson JSON string representing the inventory to save
     */
    public void setSavedInventoryJson(String savedInventoryJson) {
        this.savedInventoryJson = savedInventoryJson;
    }
}
