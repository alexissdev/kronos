package dev.alexissdev.kronos.players.domain;

import java.util.UUID;

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

    public void incrementKills() { kills++; }

    public void incrementDeaths() { deaths++; }

    public int decrementLives() {
        if (lives > 0) lives--;
        return lives;
    }

    public boolean tryRegenLife(int maxLives, long regenIntervalMs) {
        if (lives >= maxLives) return false;
        if (System.currentTimeMillis() - lastLifeRegenAt < regenIntervalMs) return false;
        lives++;
        lastLifeRegenAt = System.currentTimeMillis();
        return true;
    }

    public void setLives(int lives) { this.lives = lives; }

    public long getLastLifeRegenAt()          { return lastLifeRegenAt; }
    public void setLastLifeRegenAt(long t)    { this.lastLifeRegenAt = t; }

    public boolean isPvpTimerGiven()          { return pvpTimerGiven; }
    public void setPvpTimerGiven(boolean v)   { this.pvpTimerGiven = v; }

    public UUID   getUuid()   { return uuid; }
    public String getName()   { return name; }
    public void   setName(String name) { this.name = name; }

    public int  getKills()  { return kills; }
    public int  getDeaths() { return deaths; }
    public int  getLives()  { return lives; }

    public KitType getActiveKit()                { return activeKit; }
    public void    setActiveKit(KitType activeKit) { this.activeKit = activeKit; }

    public String getSavedInventoryJson() { return savedInventoryJson; }
    public void setSavedInventoryJson(String savedInventoryJson) {
        this.savedInventoryJson = savedInventoryJson;
    }
}
