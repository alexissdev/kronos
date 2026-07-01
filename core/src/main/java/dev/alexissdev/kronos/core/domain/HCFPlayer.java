package dev.alexissdev.kronos.core.domain;

import java.util.UUID;

public final class HCFPlayer {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private KitType activeKit;
    private boolean staffMode;
    private boolean vanished;
    private String savedInventoryJson;

    public HCFPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
        this.activeKit = KitType.DIAMOND;
        this.staffMode = false;
        this.vanished = false;
    }

    public HCFPlayer(UUID uuid, String name, int kills, int deaths,
                     KitType activeKit, boolean staffMode, boolean vanished, String savedInventoryJson) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.activeKit = activeKit;
        this.staffMode = staffMode;
        this.vanished = vanished;
        this.savedInventoryJson = savedInventoryJson;
    }

    public void incrementKills() { kills++; }

    public void incrementDeaths() { deaths++; }

    public UUID getUuid() { return uuid; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public int getKills() { return kills; }

    public int getDeaths() { return deaths; }

    public KitType getActiveKit() { return activeKit; }

    public void setActiveKit(KitType activeKit) { this.activeKit = activeKit; }

    public boolean isStaffMode() { return staffMode; }

    public void setStaffMode(boolean staffMode) { this.staffMode = staffMode; }

    public boolean isVanished() { return vanished; }

    public void setVanished(boolean vanished) { this.vanished = vanished; }

    public String getSavedInventoryJson() { return savedInventoryJson; }

    public void setSavedInventoryJson(String savedInventoryJson) {
        this.savedInventoryJson = savedInventoryJson;
    }
}
