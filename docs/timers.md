# kronos-timers

The timer system manages **per-player, time-limited restrictions** such as combat tags, PvP protection, and item cooldowns. Redis is the primary store (using native TTL for automatic expiry); MongoDB serves as a persistence backup for server restarts.

---

## Domain Model — `Timer`

`dev.alexissdev.kronos.timers.domain.Timer`

| Field | Type | Description |
|---|---|---|
| `playerUuid` | `UUID` | The player this timer belongs to |
| `type` | `TimerType` | Which restriction this timer enforces |
| `expiresAt` | `Instant` | UTC instant when the timer becomes inactive |

Key methods:

```java
boolean isExpired()          // true if Instant.now() is after expiresAt
long    getRemainingMillis() // max(0, expiresAt - now) in milliseconds
```

---

## TimerType Enum

`dev.alexissdev.kronos.timers.domain.TimerType`

| Value | Trigger | Effect while active |
|---|---|---|
| `COMBAT_TAG` | Player takes or deals PvP damage | Disconnecting causes an automatic kill; duration 30 seconds from last hit |
| `PVP_TIMER` | Player connects for the first time or returns after a deathban | Player cannot deal or receive PvP damage; cancelled if the player attacks |
| `ENDERPEARL` | Enderpearl thrown | Cannot throw another enderpearl until cooldown expires |
| `GAPPLE` | Golden apple consumed | Cannot consume another golden apple until cooldown expires |
| `HOME` | `/home` command issued | Teleport is queued; cancelled if the player moves or takes damage |
| `CLASS_COOLDOWN` | Class active ability triggered | Player cannot activate a class ability again until cooldown expires |
| `LOGOUT` | `/logout` command issued | Player must stay still and out of combat before safe disconnect |
| `STUCK` | `/stuck` command issued | Player is teleported to safety after the timer; cancelled by damage |

---

## Storage Architecture

```
┌──────────────┐   findTimer()   ┌───────────────────────┐
│  TimerCache  │◄────────────────│ RedisTimerRepository  │
│  (in-memory) │                 │ key: timer:{uuid}:{type}│
│  ConcurrentHashMap             │ value: expiresAt epoch │
└──────────────┘                 │ TTL: remainingSeconds  │
       ▲                         └───────────────────────┘
       │ markActive()                        ▲
       │                          MongoDB backup (async,
       │                          fire-and-forget via
       │                          MongoTimerBackupRepository)
       │
 synchronous lookups
 inside Bukkit event handlers
```

### Redis layer (`RedisTimerRepository`)

- Key format: `timer:{playerUuid}:{TimerType.name()}`
- Value: epoch milliseconds of `expiresAt` as a decimal string
- TTL: `remainingMillis / 1000` seconds (minimum 1)
- Expiry is fully automatic — Redis deletes the key when the TTL reaches zero
- All operations are non-blocking via Lettuce's `RedisAsyncCommands`

### In-memory cache (`TimerCache`)

- `ConcurrentHashMap<UUID, Set<TimerType>>` for O(1) active checks inside event handlers
- `ConcurrentHashMap<UUID, Map<TimerType, Long>>` storing expiry epoch ms for countdown display
- `TimerCache` is **a mirror only** — Redis is the source of truth
- A background task (`scheduleExpiryChecks`) runs every 40 ticks (2 seconds) and calls `hasActiveTimer` for each online player's cached timers, which syncs the cache with Redis and fires expired events if needed

### MongoDB backup (`MongoTimerBackupRepository`)

- Written fire-and-forget after each Redis `setex`
- Read during `loadTimersIntoCache` on player join (fallback when Redis key is missing)
- Ensures timers survive a Redis restart

---

## Event Flow

```
startTimer(uuid, COMBAT_TAG, 30_000)
  │
  ├─ TimerCache.markActive(uuid, COMBAT_TAG, expiresAt)
  ├─ EventBus.post(PlayerTimerStartedDomainEvent)    ← listeners react immediately
  └─ RedisTimerRepository.saveTimer(timer)           ← async Redis SETEX
       └─ MongoTimerBackupRepository.save(timer)     ← fire-and-forget MongoDB write

(2 seconds later, background check)
  hasActiveTimer(uuid, COMBAT_TAG)
    └─ RedisTimerRepository.findTimer(...)
         ├─ if active   → TimerCache.markActive(...)
         └─ if expired  → TimerCache.markInactive(...)
                        → EventBus.post(PlayerTimerExpiredDomainEvent)

cancelTimer(uuid, COMBAT_TAG)
  ├─ TimerCache.markInactive(uuid, COMBAT_TAG)
  ├─ EventBus.post(PlayerTimerExpiredDomainEvent) [if was active]
  ├─ MongoTimerBackupRepository.delete(uuid, COMBAT_TAG)
  └─ RedisTimerRepository.deleteTimer(uuid, COMBAT_TAG)
```

### Domain events published

| Event class | When |
|---|---|
| `PlayerTimerStartedDomainEvent` | `startTimer(...)` is called |
| `PlayerTimerExpiredDomainEvent` | `cancelTimer(...)` is called or the cache sync detects expiry |
| `PlayerCombatTaggedDomainEvent` | `tagForCombat(tagged, tagger)` is called |

---

## Guice Module — `TimersModule`

```java
bind(TimerCache.class).in(Singleton.class);
bind(TimerApplicationService.class).in(Singleton.class);
bind(TimerService.class).to(TimerApplicationService.class);
bind(TimerRepository.class).to(RedisTimerRepository.class).in(Singleton.class);
```

---

## Convenience Methods on `TimerApplicationService`

These shortcuts wrap `startTimer` with the appropriate `TimerType`:

| Method | Timer type started |
|---|---|
| `tagForCombat(tagged, tagger)` | `COMBAT_TAG` for both players (30 s) |
| `startPvpTimer(uuid, durationMs)` | `PVP_TIMER` |
| `startEnderpearlCooldown(uuid, durationMs)` | `ENDERPEARL` |
| `startGappleCooldown(uuid, durationMs)` | `GAPPLE` |
| `startLogoutTimer(uuid, durationMs)` | `LOGOUT` |
| `startHomeTimer(uuid, durationMs)` | `HOME` |
| `startStuckTimer(uuid, durationMs)` | `STUCK` |

Sync helpers (safe to call from Bukkit events — no I/O):

| Method | Description |
|---|---|
| `hasActiveTimerSync(uuid, type)` | Checks `TimerCache` only |
| `getRemainingSeconds(uuid, type)` | Reads remaining ms from `TimerCache` / 1000 |

---

## How to Add a New TimerType

### Step 1 — Add the enum value

In `kronos-timers/src/main/java/dev/alexissdev/kronos/timers/domain/TimerType.java`, add a new constant with a Javadoc explaining what restriction it enforces:

```java
/**
 * Cooldown after eating a steak — prevents spamming food during combat.
 */
STEAK_COOLDOWN,
```

### Step 2 — Add a duration constant (optional)

If the duration is fixed, add it to `TimerApplicationService` or a dedicated config binding in `RootModule`:

```java
// In RootModule.bindConfig():
bindLong("steak.cooldown-ms", config.getInt("timers.steak-cooldown-seconds", 10) * 1000L);
```

Inject the value where needed:

```java
@Named("steak.cooldown-ms") long steakCooldownMs
```

### Step 3 — Add a convenience method (optional)

In `TimerApplicationService`:

```java
public CompletableFuture<Void> startSteakCooldown(UUID playerUuid, long durationMs) {
    return startTimer(playerUuid, TimerType.STEAK_COOLDOWN, durationMs);
}
```

### Step 4 — Listen for the trigger event

Create or modify a Bukkit listener. Inject `TimerApplicationService` via Guice:

```java
@EventHandler
public void onPlayerEat(PlayerItemConsumeEvent event) {
    if (event.getItem().getType() != Material.COOKED_BEEF) return;
    Player player = event.getPlayer();
    if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.STEAK_COOLDOWN)) {
        event.setCancelled(true);
        player.sendMessage("Steak on cooldown!");
        return;
    }
    timerService.startSteakCooldown(player.getUniqueId(), steakCooldownMs);
}
```

### Step 5 — Add a scoreboard line

Add the new type to `ScoreboardRenderer.buildTimerLines`:

```java
addTimer(lines, data, TimerType.STEAK_COOLDOWN, "scoreboard.timer.steak");
```

Add the corresponding key to `messages.yml`:

```yaml
scoreboard:
  timer:
    steak: "&6Steak &7{time}"
```

### Step 6 — Register the listener

In `RootModule`, bind the listener as a singleton and register it in `PluginEnableHandler.registerListeners()`.
