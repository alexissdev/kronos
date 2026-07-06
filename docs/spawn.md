# kronos-spawn

The spawn module manages a single server-wide protected zone where PvP is disabled and players with a combat tag cannot enter.

---

## Domain Model — `SpawnZone`

`dev.alexissdev.kronos.spawn.domain.SpawnZone`

| Field | Type | Description |
|---|---|---|
| `world` | `String` | Bukkit world name where the spawn is located |
| `minX` | `int` | Minimum X coordinate of the rectangular zone (block coordinates) |
| `minZ` | `int` | Minimum Z coordinate |
| `maxX` | `int` | Maximum X coordinate |
| `maxZ` | `int` | Maximum Z coordinate |

The entity is **fully immutable** after construction. To change the spawn zone, call `SpawnService.setZone(newZone)`.

Key method:

```java
boolean contains(Location loc)
// Returns true if loc.getWorld().getName().equals(world)
//   and loc.getX() is in [minX, maxX]
//   and loc.getZ() is in [minZ, maxZ]
```

This is called from `SpawnListener` on every `PlayerMoveEvent` to check if the player has entered or left the zone. The call is synchronous and never touches the database, so it is safe on the main thread.

---

## How the Spawn Zone is Defined

Spawn zone creation uses a **wand-based** interactive session, similar to KOTH creation.

**Session flow:**

1. Admin runs `/spawn setwand` (or equivalent admin command).
2. A spawn wand item is given to the admin.
3. Admin right-clicks the **first corner** of the intended spawn boundary.
4. Admin right-clicks the **second diagonal corner**.
5. `SpawnWandListener` detects both clicks and constructs a `SpawnZone` from `Math.min` / `Math.max` of the two coordinates.
6. `SpawnService.setZone(zone)` is called, which saves to MongoDB and updates the in-memory cache.

Only one zone can be active at a time. Calling `setZone` replaces any previously configured spawn.

---

## Protections Inside Spawn

`SpawnListener` enforces the following rules for any player inside a spawn zone:

| Event | Behaviour |
|---|---|
| Player takes damage from another player | Damage cancelled (no PvP) |
| Player with `COMBAT_TAG` active tries to enter | Movement cancelled and player pushed back; informed they cannot enter while combat-tagged |
| Player attempts to place or break blocks | Action cancelled |
| Player drops items | Drop cancelled |
| Explosions inside spawn | Damage to players cancelled; block damage cancelled |

These checks use `SpawnService.getZone()` (synchronous, reads from in-memory cache) combined with `TimerApplicationService.hasActiveTimerSync(uuid, TimerType.COMBAT_TAG)`.

---

## SpawnService Interface

`dev.alexissdev.kronos.spawn.service.SpawnService`

| Method | Description |
|---|---|
| `setZone(zone)` | Persist the zone to MongoDB and update the in-memory cache |
| `removeZone()` | Delete the zone from MongoDB and clear the cache |
| `getZone()` | Synchronous read from in-memory cache; returns `Optional<SpawnZone>` |
| `loadZone()` | Async MongoDB read; populates the cache — called once during `PluginEnableHandler.enable()` |

The in-memory cache means `getZone()` has zero I/O cost and can be called safely from any Bukkit event handler without blocking the main thread.

---

## Admin Commands (`/spawn`)

| Subcommand | Permission | Description |
|---|---|---|
| `/spawn set` | `kronos.admin` | Open an interactive wand session to define the spawn zone |
| `/spawn remove` | `kronos.admin` | Remove the current spawn zone |
| `/spawn info` | `kronos.admin` | Display current zone coordinates and world |
| `/spawn tp` | `kronos.admin` | Teleport to the centre of the spawn zone |

The `/spawn` command for regular players (to teleport to spawn) is registered separately and does not require admin permission.

---

## Guice Module — `SpawnModule`

Binds `SpawnApplicationService` as the singleton `SpawnService` implementation and `SpawnRepository` to its MongoDB implementation. The service is loaded eagerly during `PluginEnableHandler.enable()` via:

```java
injector.getInstance(SpawnApplicationService.class).loadZone();
```
