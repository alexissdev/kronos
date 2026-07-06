package dev.alexissdev.kronos.factions.domain;

import java.time.Instant;
import java.util.*;

/**
 * Root aggregate representing a faction in the HCF (Hardcore Factions) system.
 *
 * <p>A faction groups a set of players under a unique name and a leader.
 * It manages its internal economy (balance), relationships with other factions (allies and enemies),
 * combat statistics (kills and deaths), the DTK (Deaths To Kick) counter, and the
 * raidable state that determines whether its claims can be overclaimed by enemies.
 *
 * <p>Key business rules:
 * <ul>
 *   <li>When {@code dtkRemaining} reaches 0, the faction becomes <em>raidable</em>.</li>
 *   <li>A <em>frozen</em> faction cannot receive new members or deposits.</li>
 *   <li>Accumulating {@value #MAX_STRIKES} strikes causes the faction to be automatically disbanded.</li>
 * </ul>
 *
 * <p>Instances of this class are persisted in MongoDB via
 * {@link dev.alexissdev.kronos.factions.persistence.MongoFactionRepository}.
 */
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
    private boolean raidable;

    /**
     * Creation constructor: initialises a brand-new faction with default values.
     *
     * <p>The DTK counter starts at {@code maxDtk}, balance at 0, with no allies or enemies
     * and in a normal state (not frozen, not raidable).
     *
     * @param id        unique faction identifier (UUID as a string)
     * @param name      visible faction name
     * @param leaderId  UUID of the player who creates and leads the faction
     * @param maxDtk    maximum number of DTK (Deaths To Kick) the faction can absorb
     * @param createdAt instant at which the faction was created
     */
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
        this.raidable = false;
        this.createdAt = createdAt;
    }

    /**
     * Reconstitution constructor: restores a faction from the database with all
     * of its already-persisted fields.
     *
     * <p>This constructor is used exclusively by
     * {@link dev.alexissdev.kronos.factions.persistence.MongoFactionRepository} when
     * deserialising MongoDB documents, and therefore accepts the full aggregate state.
     *
     * @param id           unique faction identifier
     * @param name         faction name
     * @param leaderId     UUID of the current leader
     * @param maxDtk       maximum DTK configured at faction creation
     * @param dtkRemaining DTK remaining at the time of persistence
     * @param kills        total kills accumulated by the faction
     * @param deaths       total deaths accumulated by the faction
     * @param balance      current economic balance of the faction
     * @param createdAt    original creation instant
     * @param members      member map (UUID → FactionMember) already reconstituted
     * @param allies       set of allied faction IDs
     * @param enemies      set of enemy faction IDs
     * @param strikes      number of accumulated strikes
     * @param frozen       whether the faction is frozen
     * @param raidable     whether the faction is currently raidable
     */
    public Faction(String id, String name, UUID leaderId, int maxDtk, int dtkRemaining,
                   int kills, int deaths, double balance, Instant createdAt,
                   Map<UUID, FactionMember> members, Set<String> allies, Set<String> enemies,
                   int strikes, boolean frozen, boolean raidable) {
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
        this.raidable = raidable;
    }

    /**
     * Adds a member to the faction.
     *
     * <p>If a member with the same UUID already exists, this method replaces them.
     *
     * @param member object encapsulating the new member's UUID, role, and join date
     */
    public void addMember(FactionMember member) {
        members.put(member.getUuid(), member);
    }

    /**
     * Removes the member with the given UUID from the faction.
     *
     * <p>If the UUID does not belong to any member, the operation has no effect.
     *
     * @param uuid UUID of the player to remove
     */
    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    /**
     * Finds and returns the faction member with the given UUID.
     *
     * @param uuid UUID of the player to look up
     * @return {@link Optional} containing the {@link FactionMember} if found, or empty if not a member
     */
    public Optional<FactionMember> getMember(UUID uuid) {
        return Optional.ofNullable(members.get(uuid));
    }

    /**
     * Returns whether the player with the given UUID is an active member of this faction.
     *
     * @param uuid UUID of the player
     * @return {@code true} if the player belongs to the faction
     */
    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    /**
     * Increments the faction's kill counter by one.
     *
     * <p>Invoked when a member of this faction kills an enemy player.
     */
    public void incrementKills() { kills++; }

    /**
     * Increments the faction's death counter by one.
     *
     * <p>Invoked whenever any member of the faction dies, regardless of
     * whether that death also consumes a DTK.
     */
    public void incrementDeaths() { deaths++; }

    /**
     * Decrements the remaining DTK (Deaths To Kick) counter by one.
     *
     * <p>DTK is the number of member deaths the faction can absorb before becoming raidable.
     * If the counter is already at 0, no change is made.
     *
     * @return {@code true} if the decrement succeeded; {@code false} if DTK was already 0
     */
    public boolean decrementDtk() {
        if (dtkRemaining > 0) {
            dtkRemaining--;
            return true;
        }
        return false;
    }

    /**
     * Returns whether the faction has exhausted all of its remaining DTK.
     *
     * <p>When this method returns {@code true}, the faction service will mark the
     * faction as raidable and publish the {@link dev.alexissdev.kronos.factions.event.FactionRaidableDomainEvent}.
     *
     * @return {@code true} if {@code dtkRemaining} is 0 or less
     */
    public boolean isAtDtk() { return dtkRemaining <= 0; }

    /**
     * Registers the faction with the given ID as an ally of this faction.
     *
     * @param factionId ID of the allied faction
     */
    public void addAlly(String factionId) { allies.add(factionId); }

    /**
     * Removes the faction with the given ID from the set of allies.
     *
     * @param factionId ID of the faction to unlink
     */
    public void removeAlly(String factionId) { allies.remove(factionId); }

    /**
     * Returns whether the faction with the given ID is an ally of this faction.
     *
     * @param factionId ID of the faction to check
     * @return {@code true} if both factions are allies
     */
    public boolean isAlly(String factionId) { return allies.contains(factionId); }

    /**
     * Registers the faction with the given ID as an enemy of this faction.
     *
     * @param factionId ID of the enemy faction
     */
    public void addEnemy(String factionId) { enemies.add(factionId); }

    /**
     * Removes the faction with the given ID from the set of enemies.
     *
     * @param factionId ID of the faction to unlink
     */
    public void removeEnemy(String factionId) { enemies.remove(factionId); }

    /**
     * Returns whether the faction with the given ID is an enemy of this faction.
     *
     * @param factionId ID of the faction to check
     * @return {@code true} if both factions are enemies
     */
    public boolean isEnemy(String factionId) { return enemies.contains(factionId); }

    /**
     * Increases the faction's balance by the specified amount.
     *
     * <p>This method does not validate whether the faction is frozen; that validation
     * is the responsibility of the faction service.
     *
     * @param amount amount to deposit (must be greater than 0)
     */
    public void deposit(double amount) { balance += amount; }

    /**
     * Reduces the faction's balance by the specified amount.
     *
     * <p>Does not validate whether the resulting balance would be negative; prior
     * validation is the responsibility of the faction service.
     *
     * @param amount amount to withdraw (must be greater than 0 and no more than the current balance)
     */
    public void withdraw(double amount) { balance -= amount; }

    /**
     * Adds an administrative strike to the faction.
     *
     * <p>Upon reaching {@value #MAX_STRIKES} strikes, the faction service
     * will automatically disband the faction.
     */
    public void addStrike() { strikes++; }

    /**
     * Returns whether the faction has reached the maximum allowed number of strikes.
     *
     * @return {@code true} if accumulated strikes are equal to or greater than {@value #MAX_STRIKES}
     */
    public boolean isAtMaxStrikes() { return strikes >= MAX_STRIKES; }

    /**
     * Returns the unique identifier of the faction.
     *
     * @return faction ID as a string (UUID representation)
     */
    public String getId() { return id; }

    /**
     * Returns the visible name of the faction.
     *
     * @return faction name
     */
    public String getName() { return name; }

    /**
     * Changes the visible name of the faction.
     *
     * <p>The new name must be unique on the server; uniqueness validation
     * is the responsibility of the faction service.
     *
     * @param name new faction name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the UUID of the current faction leader.
     *
     * @return UUID of the leader
     */
    public UUID getLeaderId() { return leaderId; }

    /**
     * Updates the leader of the faction.
     *
     * <p>Must be invoked together with the corresponding role changes on the affected members.
     * See {@link dev.alexissdev.kronos.factions.FactionApplicationService#setLeader}.
     *
     * @param leaderId UUID of the new leader
     */
    public void setLeaderId(UUID leaderId) { this.leaderId = leaderId; }

    /**
     * Returns an immutable view of the faction's member map.
     *
     * @return UUID → FactionMember map (unmodifiable)
     */
    public Map<UUID, FactionMember> getMembers() { return Collections.unmodifiableMap(members); }

    /**
     * Returns an immutable view of the set of allied faction IDs.
     *
     * @return set of ally IDs (unmodifiable)
     */
    public Set<String> getAllies() { return Collections.unmodifiableSet(allies); }

    /**
     * Returns an immutable view of the set of enemy faction IDs.
     *
     * @return set of enemy IDs (unmodifiable)
     */
    public Set<String> getEnemies() { return Collections.unmodifiableSet(enemies); }

    /**
     * Returns the current economic balance of the faction.
     *
     * @return balance in the server's currency
     */
    public double getBalance() { return balance; }

    /**
     * Returns the total kills accumulated by the faction's members.
     *
     * @return number of kills
     */
    public int getKills() { return kills; }

    /**
     * Returns the total deaths accumulated by the faction's members.
     *
     * @return number of deaths
     */
    public int getDeaths() { return deaths; }

    /**
     * Returns the DTK remaining before the faction becomes raidable.
     *
     * @return remaining DTK (0 indicates raidable state)
     */
    public int getDtkRemaining() { return dtkRemaining; }

    /**
     * Returns the maximum DTK the faction was configured with.
     *
     * @return maximum DTK
     */
    public int getMaxDtk() { return maxDtk; }

    /**
     * Returns the instant at which the faction was created.
     *
     * @return creation timestamp
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * Returns the faction's home location, or {@code null} if none has been set.
     *
     * @return home location, or {@code null}
     */
    public FactionHome getHome() { return home; }

    /**
     * Sets a new home location for the faction.
     *
     * @param home new home location; may be {@code null} to clear the home
     */
    public void setHome(FactionHome home) { this.home = home; }

    /**
     * Clears the faction's home, leaving the field as {@code null}.
     */
    public void clearHome() { this.home = null; }

    /**
     * Returns the number of administrative strikes accumulated by the faction.
     *
     * @return current number of strikes
     */
    public int getStrikes() { return strikes; }

    /**
     * Returns the maximum number of strikes a faction can accumulate before being disbanded.
     *
     * @return strike limit ({@value #MAX_STRIKES})
     */
    public int getMaxStrikes() { return MAX_STRIKES; }

    /**
     * Returns whether the faction is frozen.
     *
     * <p>A frozen faction cannot receive new members or economic deposits.
     *
     * @return {@code true} if the faction is frozen
     */
    public boolean isFrozen() { return frozen; }

    /**
     * Changes the frozen state of the faction.
     *
     * @param frozen {@code true} to freeze the faction; {@code false} to unfreeze it
     */
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    /**
     * Returns whether the faction is currently raidable, meaning its claims
     * can be overclaimed by enemy factions.
     *
     * @return {@code true} if the faction is raidable
     */
    public boolean isRaidable() { return raidable; }

    /**
     * Changes the raidable state of the faction.
     *
     * <p>This method is called when the DTK reaches 0 (to activate the state)
     * or when an administrator manually restores the faction's DTK.
     *
     * @param raidable {@code true} to mark the faction as raidable
     */
    public void setRaidable(boolean raidable) { this.raidable = raidable; }
}
