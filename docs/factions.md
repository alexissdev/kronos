# kronos-factions

The faction system is the core of Hardcore Factions gameplay. It manages player groups, their territory power (DTK), diplomatic relations, economy, and the raid lifecycle.

---

## Domain Model — `Faction`

`dev.alexissdev.kronos.factions.domain.Faction`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Immutable UUID-as-string assigned at creation |
| `name` | `String` | Public display name (mutable by leader) |
| `leaderId` | `UUID` | Current leader's UUID |
| `members` | `Map<UUID, FactionMember>` | All members keyed by UUID |
| `allies` | `Set<String>` | IDs of allied factions |
| `enemies` | `Set<String>` | IDs of enemy factions |
| `balance` | `double` | Faction bank balance |
| `kills` | `int` | Cumulative kills by all members |
| `deaths` | `int` | Cumulative deaths by all members |
| `dtkRemaining` | `int` | Deaths-To-Kick counter (starts at `maxDtk`) |
| `maxDtk` | `int` | Max DTK when faction was created (default: 20) |
| `createdAt` | `Instant` | UTC creation timestamp |
| `home` | `FactionHome` | Optional teleport home location |
| `strikes` | `int` | Admin-applied strike count (max 3 before auto-disband) |
| `frozen` | `boolean` | If true, no new members and no deposits |
| `raidable` | `boolean` | If true, enemies can overclaim territory |

### Key Business Methods

```java
boolean decrementDtk()          // returns false if DTK already 0
boolean isAtDtk()               // true when dtkRemaining <= 0
boolean isAtMaxStrikes()        // true when strikes >= 3
void    addMember(FactionMember)
void    removeMember(UUID)
Optional<FactionMember> getMember(UUID)
void    addAlly(String factionId)
void    addEnemy(String factionId)
void    deposit(double amount)
void    withdraw(double amount)
```

---

## FactionRole Hierarchy

`dev.alexissdev.kronos.factions.domain.FactionRole`

| Role | Priority | Permissions |
|---|---|---|
| `LEADER` | 4 | All permissions; can disband, transfer leadership, rename |
| `CO_LEADER` | 3 | Can set roles (below own level), manage relations, withdraw funds |
| `CAPTAIN` | 2 | Can invite, kick members, set/clear faction home, claim territory |
| `MEMBER` | 1 | No administrative permissions |

Permission checks use `FactionRole.isAtLeast(required)`:

```java
// Actor must be CAPTAIN or higher:
if (!actor.getRole().isAtLeast(FactionRole.CAPTAIN)) {
    throw new FactionPermissionException(required);
}
```

A CO_LEADER cannot assign a role equal to or higher than their own.

---

## FactionMember

`dev.alexissdev.kronos.factions.domain.FactionMember`

| Field | Type | Description |
|---|---|---|
| `uuid` | `UUID` | Player UUID |
| `role` | `FactionRole` | Current role (mutable by higher-rank member) |
| `joinedAt` | `Instant` | When the player joined |

`FactionMember` instances live **embedded inside the `Faction` aggregate** and are not persisted independently.

---

## DTK Mechanic (Deaths To Kick)

DTK is the primary mechanic that governs when a faction can be raided.

**Flow:**

1. A faction member dies in PvP.
2. `FactionApplicationService.notifyMemberDeath(factionId, deadMemberUuid)` is called.
3. `faction.incrementDeaths()` is called unconditionally.
4. If the dead player is a faction member, `faction.decrementDtk()` is called.
5. If `faction.isAtDtk()` returns `true` and the faction is not yet raidable:
   - `faction.setRaidable(true)`
   - `EventBus.post(new FactionRaidableDomainEvent(factionId, name))`
6. `EventBus.post(new FactionDtkDecrementedDomainEvent(...))` is always posted when DTK changes.

When a faction is raidable, enemies can call `ClaimApplicationService.overclaim(...)` on its chunks.

---

## Ally and Enemy Relations

Relations are **bidirectional** and stored in each faction's `allies` / `enemies` sets.

### Setting an alliance (`setAlly`)

1. Both factions are loaded in parallel.
2. Any existing enemy relation between them is removed on both sides.
3. Each faction adds the other to its `allies` set.
4. Both factions are saved.

### Setting enmity (`setEnemy`)

Same as above but populates `enemies` and removes from `allies`.

### Removing a relation (`removeRelation`)

Removes both the ally and enemy entry on both sides, returning them to neutral.

**Ally protection:** while SOTW is not active, allies cannot deal damage to each other (enforced in `PvpListener`).

---

## Faction Bank

The bank allows members to pool resources for territory expansion and admin fees.

| Operation | Minimum role | Description |
|---|---|---|
| `deposit(factionId, playerUuid, amount)` | `MEMBER` | Withdraws from player via `EconomyService.withdraw`, adds to `faction.balance` |
| `withdraw(factionId, playerUuid, amount)` | `CO_LEADER` | Deducts from `faction.balance`, deposits to player via `EconomyService.deposit` |

Deposits fail if the faction is frozen. Withdrawals fail if `faction.balance < amount` (throws `InsufficientFundsException`).

---

## Invite and Cooldown System

Invites are stored in two `ConcurrentHashMap` instances on `FactionApplicationService` (not persisted across restarts):

| Map | Key | Value | Purpose |
|---|---|---|---|
| `pendingInvites` | invitee UUID | factionId | Tracks which faction invited the player |
| `inviteTimestamps` | invitee UUID | `System.currentTimeMillis()` | Tracks when the invite was sent |
| `leftFactionTimestamps` | player UUID | `System.currentTimeMillis()` | Used for re-invite cooldown |
| `leftFactionIds` | player UUID | factionId | Tracks which faction the player left |

**Validation on `inviteMember`:**

- Actor must be `CAPTAIN` or higher.
- Faction must not be frozen.
- Faction must not be full (max members from `@Named("faction.max-members")`).
- Invitee must not already be in any faction.
- Re-invite cooldown (`@Named("faction.reinvite-cooldown-ms")`) must have elapsed since the invitee last left this faction.

**Validation on `acceptInvite`:**

- A pending invite from the target faction must exist.
- The invite must not have expired (`@Named("faction.invite-expiry-ms")`).
- The faction must still have room.

---

## Domain Events

| Event | Trigger | Fields |
|---|---|---|
| `FactionCreatedDomainEvent` | `createFaction(...)` | `factionId`, `factionName`, `leaderUuid` |
| `FactionDisbandedDomainEvent` | `disbandFaction(...)` or max strikes | `factionId`, `factionName`, `actorUuid` |
| `FactionRaidableDomainEvent` | DTK reaches 0 | `factionId`, `factionName` |
| `FactionDtkDecrementedDomainEvent` | `notifyMemberDeath(...)` | `factionId`, `factionName`, `dtkRemaining`, `maxDtk` |
| `PlayerJoinedFactionDomainEvent` | `acceptInvite(...)` | `playerUuid`, `factionId` |
| `PlayerLeftFactionDomainEvent` | `kickMember(...)` or `leaveFaction(...)` | `playerUuid`, `factionId`, `wasKicked` |

---

## Persistence — `FactionRepository`

`dev.alexissdev.kronos.factions.repository.FactionRepository`

Implemented by `MongoFactionRepository`. All methods are async:

| Method | Description |
|---|---|
| `findById(id)` | Find by internal ID |
| `findByName(name)` | Case-insensitive name lookup |
| `findByMember(playerUuid)` | Find the faction containing the player |
| `findTopByKills(limit)` | Top N factions ordered by kills desc |
| `findRaidable()` | All factions with `raidable = true` |
| `save(faction)` | Upsert |
| `delete(id)` | Hard delete |

---

## Guide: How to Add a New Faction Action

### Example: `/f sethome` (already exists, shown as template)

**1. Define business method in `FactionService` interface:**

```java
CompletableFuture<Void> myNewAction(String factionId, UUID actorUuid, /* params */);
```

**2. Implement in `FactionApplicationService`:**

```java
@Override
public CompletableFuture<Void> myNewAction(String factionId, UUID actorUuid, /* params */) {
    return factionRepository.findById(factionId).thenCompose(opt -> {
        Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
        requireRole(faction, actorUuid, FactionRole.CAPTAIN);  // or whatever minimum
        // mutate faction state
        faction.someMethod(/* ... */);
        return factionRepository.save(faction).thenRun(() ->
                eventBus.post(new MyNewFactionDomainEvent(factionId, /* ... */)));
    });
}
```

**3. Create the domain event class** (if needed) in `dev.alexissdev.kronos.factions.event`:

```java
public final class MyNewFactionDomainEvent {
    private final String factionId;
    // constructor + getter
}
```

**4. Create a `SubCommand` class** in `kronos-plugin`:

```java
public class MyNewFactionSub extends SubCommand {
    private final FactionService factionService;

    @Inject
    public MyNewFactionSub(FactionService factionService) {
        this.factionService = factionService;
    }

    @Override public String name() { return "mynewaction"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        // call factionService.myNewAction(...)
        //   .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("Done")))
        //   .exceptionally(ex -> { /* handle errors */ return null; });
    }
}
```

**5. Register in `RootModule.bindFactionSubCommands()`:**

```java
binder.addBinding().to(MyNewFactionSub.class).in(Singleton.class);
```
