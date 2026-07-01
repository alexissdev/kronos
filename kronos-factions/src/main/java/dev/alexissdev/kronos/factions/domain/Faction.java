package dev.alexissdev.kronos.factions.domain;

import java.time.Instant;
import java.util.*;

public final class Faction {

    private static final int MAX_STRIKES = 3;

    private final String id;
    private String name;
    private UUID leaderId;
    private final Map<UUID, FactionMember> members;
    private final Set<String> allies;
    private final Set<String> enemies;
    private double balance;
    private int kills;
    private int deaths;
    private int dtkRemaining;
    private final int maxDtk;
    private final Instant createdAt;
    private FactionHome home;
    private int strikes;
    private boolean frozen;

    public Faction(String id, String name, UUID leaderId, int maxDtk, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.maxDtk = maxDtk;
        this.dtkRemaining = maxDtk;
        this.members = new LinkedHashMap<>();
        this.allies = new HashSet<>();
        this.enemies = new HashSet<>();
        this.balance = 0.0;
        this.kills = 0;
        this.deaths = 0;
        this.strikes = 0;
        this.frozen = false;
        this.createdAt = createdAt;
    }

    public Faction(String id, String name, UUID leaderId, int maxDtk, int dtkRemaining,
                   int kills, int deaths, double balance, Instant createdAt,
                   Map<UUID, FactionMember> members, Set<String> allies, Set<String> enemies,
                   int strikes, boolean frozen) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.maxDtk = maxDtk;
        this.dtkRemaining = dtkRemaining;
        this.kills = kills;
        this.deaths = deaths;
        this.balance = balance;
        this.createdAt = createdAt;
        this.members = members;
        this.allies = allies;
        this.enemies = enemies;
        this.strikes = strikes;
        this.frozen = frozen;
    }

    public void addMember(FactionMember member) {
        members.put(member.getUuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public Optional<FactionMember> getMember(UUID uuid) {
        return Optional.ofNullable(members.get(uuid));
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public void incrementKills() { kills++; }

    public void incrementDeaths() { deaths++; }

    public boolean decrementDtk() {
        if (dtkRemaining > 0) {
            dtkRemaining--;
            return true;
        }
        return false;
    }

    public boolean isAtDtk() { return dtkRemaining <= 0; }

    public void addAlly(String factionId) { allies.add(factionId); }

    public void removeAlly(String factionId) { allies.remove(factionId); }

    public boolean isAlly(String factionId) { return allies.contains(factionId); }

    public void addEnemy(String factionId) { enemies.add(factionId); }

    public void removeEnemy(String factionId) { enemies.remove(factionId); }

    public boolean isEnemy(String factionId) { return enemies.contains(factionId); }

    public void deposit(double amount) { balance += amount; }

    public void withdraw(double amount) { balance -= amount; }

    public void addStrike() { strikes++; }

    public boolean isAtMaxStrikes() { return strikes >= MAX_STRIKES; }

    public String getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public UUID getLeaderId() { return leaderId; }

    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    public Map<UUID, FactionMember> getMembers() { return Collections.unmodifiableMap(members); }

    public Set<String> getAllies() { return Collections.unmodifiableSet(allies); }

    public Set<String> getEnemies() { return Collections.unmodifiableSet(enemies); }

    public double getBalance() { return balance; }

    public int getKills() { return kills; }

    public int getDeaths() { return deaths; }

    public int getDtkRemaining() { return dtkRemaining; }

    public int getMaxDtk() { return maxDtk; }

    public Instant getCreatedAt() { return createdAt; }

    public FactionHome getHome() { return home; }

    public void setHome(FactionHome home) { this.home = home; }

    public void clearHome() { this.home = null; }

    public int getStrikes() { return strikes; }

    public int getMaxStrikes() { return MAX_STRIKES; }

    public boolean isFrozen() { return frozen; }

    public void setFrozen(boolean frozen) { this.frozen = frozen; }
}
