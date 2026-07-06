# kronos-api

The `kronos-api` module is the **public contract** for external plugins that want to integrate with Kronos without depending on internal domain classes. It exposes read-only facades registered in Bukkit's `ServicesManager`.

---

## Purpose

External plugins must never import internal classes from `kronos-factions`, `kronos-timers`, etc. Instead they depend only on `kronos-api`, which provides:

- Stable interfaces that will not break between minor versions.
- Immutable snapshot value objects (no risk of mutating internal state).
- Synchronous-from-the-caller methods (internally resolve `CompletableFuture` with a blocking join to keep the external plugin's code simple).

---

## Obtaining the API

Register a `Depend: [KronosHCF]` entry in your `plugin.yml`, then:

```java
import dev.alexissdev.kronos.api.HCFApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MyPlugin extends JavaPlugin {

    private HCFApi hcfApi;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<HCFApi> rsp =
                Bukkit.getServicesManager().getRegistration(HCFApi.class);
        if (rsp == null) {
            getLogger().severe("KronosHCF not found — disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        hcfApi = rsp.getProvider();
    }
}
```

---

## HCFApi Interface

`dev.alexissdev.kronos.api.HCFApi`

| Method | Return type | Description |
|---|---|---|
| `factions()` | `FactionApi` | Faction lookup and relation queries |
| `players()` | `PlayerDataApi` | Player statistics and connection state |
| `timers()` | `TimerApi` | Active timer state (combat tag, PvP, enderpearl) |
| `claims()` | `ClaimApi` | Territory zone queries |
| `koth()` | `KothApi` | KOTH event state queries |
| `getVersion()` | `String` | Semantic version string, e.g. `"1.0.0-SNAPSHOT"` |

---

## Facades

### FactionApi

`dev.alexissdev.kronos.api.facade.FactionApi`

All operations are synchronous from the caller's perspective. Do **not** call them from async threads to avoid blocking the main server thread.

| Method | Parameters | Return | Description |
|---|---|---|---|
| `getByPlayer` | `UUID playerUuid` | `Optional<FactionSnapshot>` | Faction the player belongs to |
| `getById` | `String factionId` | `Optional<FactionSnapshot>` | Faction by internal ID |
| `getByName` | `String name` | `Optional<FactionSnapshot>` | Faction by public name (case-sensitive) |
| `getTopFactions` | `int limit` | `List<FactionSnapshot>` | Top N factions ordered by score descending |
| `isInFaction` | `UUID playerUuid` | `boolean` | Whether the player is in any faction |
| `areAllies` | `String factionIdA, String factionIdB` | `boolean` | Whether the two factions are allied (bidirectional) |
| `areEnemies` | `String factionIdA, String factionIdB` | `boolean` | Whether faction A has declared faction B as an enemy |

---

### TimerApi

`dev.alexissdev.kronos.api.facade.TimerApi`

| Method | Parameters | Return | Description |
|---|---|---|---|
| `hasCombatTag` | `UUID uuid` | `boolean` | Whether the player currently has a combat tag active |
| `hasPvpTimer` | `UUID uuid` | `boolean` | Whether the player has an active PvP protection timer |
| `hasEnderpearlCooldown` | `UUID uuid` | `boolean` | Whether the player has an active enderpearl cooldown |
| `getRemainingMillis` | `UUID uuid, TimerType type` | `OptionalLong` | Milliseconds remaining for the given timer type |
| `getTimer` | `UUID uuid, TimerType type` | `Optional<TimerSnapshot>` | Full snapshot of the active timer |

`TimerType` is imported from `dev.alexissdev.kronos.timers.domain.TimerType`. See [timers.md](timers.md) for all enum values.

---

### ClaimApi

`dev.alexissdev.kronos.api.facade.ClaimApi`

| Method | Parameters | Return | Description |
|---|---|---|---|
| `getClaimAt` | `World world, int chunkX, int chunkZ` | `Optional<ClaimSnapshot>` | Claim at the given chunk coordinates |
| `getClaimTypeAt` | `Location location` | `ClaimType` | Zone type at an exact Bukkit location |
| `isClaimed` | `World world, int chunkX, int chunkZ` | `boolean` | Whether the chunk has any claim (returns `false` for Wilderness) |
| `isWilderness` | `Location location` | `boolean` | Whether the location is unclaimed terrain |
| `isSafeZone` | `Location location` | `boolean` | Whether the location is inside a SafeZone |
| `isWarZone` | `Location location` | `boolean` | Whether the location is inside a WarZone |

---

### KothApi

`dev.alexissdev.kronos.api.facade.KothApi`

| Method | Parameters | Return | Description |
|---|---|---|---|
| `getActiveKoths` | — | `List<KothSnapshot>` | All currently running KOTH zones |
| `getKoth` | `String name` | `Optional<KothSnapshot>` | KOTH zone by name (case-sensitive) |
| `isKothActive` | `String name` | `boolean` | Whether the named KOTH is currently running |

---

### PlayerDataApi

`dev.alexissdev.kronos.api.facade.PlayerDataApi`

| Method | Parameters | Return | Description |
|---|---|---|---|
| `getPlayer` | `UUID uuid` | `Optional<PlayerSnapshot>` | Full player data snapshot |
| `getKills` | `UUID uuid` | `int` | Total kills; returns `0` if player is unknown |
| `getDeaths` | `UUID uuid` | `int` | Total deaths; returns `0` if player is unknown |
| `getBalance` | `UUID uuid` | `double` | Current balance from the economy service |
| `isOnline` | `UUID uuid` | `boolean` | Whether the player is currently connected |

---

## Snapshot Classes

Snapshots are **immutable** value objects. They capture state at the moment of the query and do not update automatically.

### TimerSnapshot

`dev.alexissdev.kronos.api.model.TimerSnapshot`

| Field | Type | Description |
|---|---|---|
| `playerUuid` | `UUID` | Owner of the timer |
| `type` | `TimerType` | Which timer this snapshot represents |
| `expiresAt` | `Instant` | UTC instant when the timer expires |
| `remainingMillis` | `long` | Milliseconds remaining **at query time** |

`getRemainingMillis()` does not decrease over time — re-query `TimerApi` for live countdowns.

---

### FactionSnapshot

`dev.alexissdev.kronos.api.model.FactionSnapshot`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Immutable internal faction ID |
| `name` | `String` | Public faction name |
| `leaderUuid` | `UUID` | UUID of the current leader |
| `memberUuids` | `List<UUID>` | Unmodifiable list of all members |
| `memberRoles` | `Map<UUID, String>` | UUID to role name (e.g. `"LEADER"`, `"CAPTAIN"`) |
| `kills` | `int` | Cumulative faction kills |
| `deaths` | `int` | Cumulative faction deaths |
| `dtkRemaining` | `int` | Deaths To Kick remaining (0 = raidable) |
| `balance` | `double` | Current faction bank balance |
| `createdAt` | `Instant` | UTC creation time |

---

### ClaimSnapshot

`dev.alexissdev.kronos.api.model.ClaimSnapshot`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique claim ID |
| `factionId` | `String` | Owner faction ID; `null` for system zones |
| `type` | `ClaimType` | Zone category |
| `world` | `String` | Bukkit world name |
| `minChunkX` / `maxChunkX` | `int` | Chunk X bounds |
| `minChunkZ` / `maxChunkZ` | `int` | Chunk Z bounds |

Chunk coordinates multiply by 16 to obtain block coordinates.

---

## Code Example — Checking Combat Tag Before Teleport

```java
import dev.alexissdev.kronos.api.HCFApi;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TeleportGuard implements Listener {

    private final HCFApi api;

    public TeleportGuard(HCFApi api) {
        this.api = api;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!event.getMessage().startsWith("/home")) return;

        if (api.timers().hasCombatTag(player.getUniqueId())) {
            event.setCancelled(true);
            long seconds = api.timers()
                    .getRemainingMillis(player.getUniqueId(), TimerType.COMBAT_TAG)
                    .orElse(0L) / 1000L;
            player.sendMessage("You are combat-tagged! (" + seconds + "s remaining)");
        }
    }
}
```

---

## Notes for Plugin Authors

- Prefer `getByPlayer(UUID)` over iterating `getTopFactions` to find a single player's faction.
- Snapshot objects become stale immediately. Never cache them across ticks.
- `areEnemies` is unidirectional: A may consider B an enemy while B considers A neutral. Check both directions if needed.
- The `KothApi` returns `KothSnapshot` objects; the `isActive()` field on the snapshot reflects the state at query time.
