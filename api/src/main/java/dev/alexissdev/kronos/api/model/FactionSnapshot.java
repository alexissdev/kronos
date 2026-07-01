package dev.alexissdev.kronos.api.model;

import java.time.Instant;
import java.util.*;

/** Immutable read-only view of a Faction for external plugin use. */
public final class FactionSnapshot {

    private final String id;
    private final String name;
    private final UUID leaderUuid;
    private final List<UUID> memberUuids;
    private final Map<UUID, String> memberRoles;
    private final int kills;
    private final int deaths;
    private final int dtkRemaining;
    private final double balance;
    private final Instant createdAt;

    public FactionSnapshot(String id, String name, UUID leaderUuid,
                           List<UUID> memberUuids, Map<UUID, String> memberRoles,
                           int kills, int deaths, int dtkRemaining,
                           double balance, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.memberUuids = Collections.unmodifiableList(new ArrayList<>(memberUuids));
        this.memberRoles = Collections.unmodifiableMap(new LinkedHashMap<>(memberRoles));
        this.kills = kills;
        this.deaths = deaths;
        this.dtkRemaining = dtkRemaining;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public UUID getLeaderUuid() { return leaderUuid; }
    public List<UUID> getMemberUuids() { return memberUuids; }
    public Map<UUID, String> getMemberRoles() { return memberRoles; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getDtkRemaining() { return dtkRemaining; }
    public double getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
}
