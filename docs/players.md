# kronos-players

The players module manages persistent HCF player profiles, the deathban system, crate locations, and the interface between the player domain and the economy/timer modules.

---

## Domain Model — `HCFPlayer`

`dev.alexissdev.kronos.players.domain.HCFPlayer`

| Field | Type | Description |
|---|---|---|
| `uuid` | `UUID` | Immutable Minecraft player UUID |
| `name` | `String` | Last known username; updated on each login |
| `kills` | `int` | Cumulative PvP kills |
| `deaths` | `int` | Cumulative PvP deaths |
| `lives` | `int` | Current lives remaining; starts at 3 |
| `pvpTimerGiven` | `boolean` | Whether the PvP protection timer was already granted this login session |
| `activeKit` | `KitType` | Currently selected combat class; default `DIAMOND` |
| `savedInventoryJson` | `String` | Serialized inventory JSON (nullable) |
| `lastLifeRegenAt` | `long` | Epoch ms of the last life regeneration tick |

### Domain Methods

```java
void incrementKills()                                        // called on kill
void incrementDeaths()                                       // called on death
int  decrementLives()                                        // returns remaining lives (min 0)
boolean tryRegenLife(int maxLives, long regenIntervalMs)     // returns true if a life was added
void setLives(int lives)                                     // used to restore lives after deathban expiry
```

New players are initialized with 3 lives, 0 kills, 0 deaths, no saved inventory, and `KitType.DIAMOND`.

---

## Load / Save Strategy

```
Player joins → PlayerApplicationService.getOrCreate(uuid, name)
  └─ PlayerRepository.findByUuid(uuid)    ← MongoDB async
       ├─ if found:
       │    - update name if changed
       │    - if lives == 0 → setLives(defaultLives)  (deathban expired)
       │    - save if dirty
       └─ if not found:
            - new HCFPlayer(uuid, name) with defaults
            - PlayerRepository.save(newPlayer)

Player kills another → PlayerApplicationService.recordKill(killerUuid, victimUuid)
  ├─ fetch both profiles in parallel (thenCombine)
  ├─ killer.incrementKills()
  ├─ victim.incrementDeaths()
  └─ save both in sequence

Player leaves → save current profile (called by PlayerDataListener on quit event)
```

Profiles are persisted in MongoDB. There is no Redis layer for player profiles themselves (only the deathban state lives in Redis).

---

## Deathban Mechanic

When a player runs out of lives (`lives == 0`), they are subject to a deathban: they cannot connect to the server until the ban expires.

### How it works

1. Player dies in PvP.
2. `PvpListener` calls `PlayerApplicationService.decrementLives(victimUuid)`.
3. If the returned lives count is `0`, the `DeathbanListener` sets a Redis entry via `DeathbanRepository` with a TTL equal to `@Named("hcf.deathban-seconds")` (default: 24 hours).
4. On the next connection attempt, `DeathbanListener` checks `PlayerApplicationService.isDeathbanned(uuid)`. If `true`, the player is kicked with a remaining-time message.
5. When the Redis key's TTL expires, the deathban is over. On the next login, `getOrCreate` detects `lives == 0` and restores them to `defaultLives`.

### Admin removal

```java
playerService.removeDeathban(uuid)  // deletes the Redis key immediately
```

After removal the player can connect again; lives are restored on the next login via `getOrCreate`.

### Deathban repository

`DeathbanRepository` stores a single Redis key per UUID:

- Key: `deathban:{uuid}`
- TTL: configurable in seconds (`hcf.deathban-seconds`)
- `getRemainingSeconds(uuid)` → `OptionalLong` with seconds remaining
- `removeDeathban(uuid)` → deletes the key

---

## Crate System

Crates are reward chests placed at specific block coordinates. When a player right-clicks a crate block, a reward inventory opens.

### CrateLocation

`dev.alexissdev.kronos.players.domain.CrateLocation`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID-as-string |
| `world` | `String` | Bukkit world name |
| `x`, `y`, `z` | `int` | Block coordinates |
| `type` | `CrateType` | Determines reward table |

### CrateType Values

Defined in `dev.alexissdev.kronos.common.domain.CrateType`:

| Value | Obtained by |
|---|---|
| `KOTH` | Capturing a KOTH event |
| `VOTE` | Voting for the server on listing sites |
| `RANK` | Donation rank rewards or high-value achievements |
| `EVENT` | Staff-organized temporary events |

### CrateService Interface

`dev.alexissdev.kronos.players.service.CrateService`

| Method | Description |
|---|---|
| `setCrate(world, x, y, z, type)` | Register or update a crate at coordinates |
| `removeCrate(world, x, y, z)` | Delete the crate at coordinates; throws `HCFException` if none |
| `getCrateAt(world, x, y, z)` | Check if a specific block is a registered crate |
| `getAllCrates()` | List all crates; used at startup to preload the `CrateListener` cache |

### Crate Opening

`CrateListener` maintains an in-memory `Set<String>` of active crate coordinate strings for O(1) block-click lookups. When a player right-clicks a crate:

1. `getCrateAt` confirms the block is a registered crate.
2. A reward inventory (`Inventory`) is opened with random items from the crate's reward table.
3. The interaction is logged.

### Admin Commands (`/crate`)

| Subcommand | Description |
|---|---|
| `/crate set <type>` | Register the block you are looking at as a crate |
| `/crate remove` | Remove the crate at the block you are looking at |
| `/crate list` | List all registered crates |

---

## Kit System (Interface with kronos-classes)

`HCFPlayer.activeKit` stores the player's current class as a `KitType`. When the player changes helmet:

1. `ClassListener` detects the `PlayerArmorStandsManipulateEvent` / `InventoryClickEvent`.
2. `KitApplicationService.updateActiveKit(uuid, newKitType)` is called.
3. The updated `KitType` is saved back to the `HCFPlayer` in MongoDB via `PlayerRepository`.

Class abilities are applied by `ClassListener` (in `kronos-classes`) based on `HCFPlayer.activeKit`.

---

## PlayerService Interface

`dev.alexissdev.kronos.players.service.PlayerService`

| Method | Description |
|---|---|
| `getOrCreate(uuid, name)` | Load or create a player profile; restores lives after expired deathban |
| `getPlayer(uuid)` | Non-creating lookup |
| `savePlayer(player)` | Explicit save (for external modifications) |
| `recordKill(killerUuid, victimUuid)` | Increment kills/deaths and save both profiles |
| `decrementLives(uuid)` | Decrement lives and save; returns remaining count |
| `isDeathbanned(uuid)` | Check Redis TTL via `DeathbanRepository` |
| `removeDeathban(uuid)` | Delete deathban key from Redis |

---

## Guice Module — `PlayersModule`

Binds `PlayerApplicationService` as the `PlayerService` singleton, `DeathbanRepository` to its Redis implementation, and `CrateApplicationService` as the `CrateService` singleton. Injects `@Named("hcf.lives")` for the default lives count.
