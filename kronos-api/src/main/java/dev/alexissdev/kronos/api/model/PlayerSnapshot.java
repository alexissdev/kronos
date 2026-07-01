package dev.alexissdev.kronos.api.model;

import java.util.UUID;

/** Immutable read-only view of a HCFPlayer for external plugin use. */
public final class PlayerSnapshot {

    private final UUID uuid;
    private final String name;
    private final int kills;
    private final int deaths;
    private final double balance;
    private final boolean online;

    public PlayerSnapshot(UUID uuid, String name, int kills, int deaths, double balance, boolean online) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.balance = balance;
        this.online = online;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public double getBalance() { return balance; }
    public boolean isOnline() { return online; }
}
