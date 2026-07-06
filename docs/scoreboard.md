# kronos-scoreboard

The scoreboard module renders a persistent sidebar scoreboard for every online player. It combines per-player live data (timers, faction info) with server-wide state (active KOTHs, SOTW/EOTW countdowns).

---

## Architecture

```
ScoreboardTask  (schedules two BukkitRunnables)
  │
  ├─ every 20 ticks (1 s) — main thread:
  │    ScoreboardManager.tickAll()
  │         └─ for each online player:
  │              ├─ update SOTW/EOTW remaining ms in PlayerBoardData
  │              └─ ScoreboardRenderer.render(player, data, activeKoths)
  │                    └─ PlayerBoard.render(lines)  ← updates Bukkit Scoreboard API
  │
  └─ every 100 ticks (5 s) — async thread:
       ScoreboardManager.refreshAllStats()
            └─ for each online player (async):
                 ├─ PlayerService.getPlayer(uuid)       → data.setKills / data.setDeaths
                 ├─ EconomyService.getBalance(uuid)     → data.setBalance
                 └─ FactionService.getByPlayer(uuid)    → data.setFactionName / data.setDtkRemaining
```

### Components

| Class | Role |
|---|---|
| `ScoreboardManager` | Lifecycle manager; owns `PlayerBoard` and `PlayerBoardData` maps; subscribes to EventBus |
| `ScoreboardTask` | Schedules `tickAll()` (1 s, main) and `refreshAllStats()` (5 s, async) |
| `ScoreboardRenderer` | Stateless renderer; converts `PlayerBoardData` + `KothEntry` collection into a `List<String>` |
| `PlayerBoard` | Thin Bukkit `Scoreboard` wrapper; calls the Scoreboard API to update lines without flicker |
| `PlayerBoardData` | Mutable per-player snapshot with `volatile` fields for cross-thread visibility |
| `KothEntry` | Immutable record: `name`, `centerX`, `centerZ`, `captureTimeSeconds` |

---

## Update Cadence

| Data | Frequency | Thread |
|---|---|---|
| Timer countdowns (combat tag, PvP, enderpearl, etc.) | Immediate on `PlayerTimerStartedDomainEvent` / `PlayerTimerExpiredDomainEvent` | Main (via `BukkitScheduler.runTask`) |
| KOTH presence and capture time | Immediate on `KothStartedDomainEvent` / `KothCapturedDomainEvent` / `KothEndedDomainEvent` | Main |
| SOTW / EOTW countdown | Every 1 second | Main |
| Kills, deaths, balance, faction | Every 5 seconds | Async |

This split avoids expensive database calls on every tick while keeping countdowns pixel-accurate.

---

## Sections Displayed (in Order)

1. Separator line (from `messages.yml` key `scoreboard.separator`)
2. **Faction & DTK** — faction name and remaining DTK counter; or "No faction" message
3. **Combat stats** — kills, deaths, economic balance (formatted as `1.5K`, `2.3M`, etc.)
4. **Active KOTHs** — one block per KOTH: name, center coordinates, capture timer (personalised for the player if they are actively capturing)
5. **Active timers** — one line per active timer type (COMBAT_TAG, PVP_TIMER, ENDERPEARL, GAPPLE, HOME, LOGOUT, CLASS_COOLDOWN, STUCK); hidden when the timer is not active
6. **SOTW / EOTW** — shown only when the respective global period is active
7. Footer line (from `messages.yml` key `scoreboard.footer`)

All templates are retrieved from `MessagesConfig`. Format placeholders use `{key}` syntax, resolved via `MessagesConfig.format(key, "placeholder", value, ...)`.

---

## EventBus Integration

`ScoreboardManager` registers itself on the Guava `EventBus` in its constructor:

```java
eventBus.register(this);
```

It then reacts to three categories of events:

### Timer events

```java
@Subscribe
public void onTimerStarted(PlayerTimerStartedDomainEvent event) {
    PlayerBoardData data = cache.get(event.getPlayerUuid());
    if (data == null) return;
    data.setTimer(event.getTimerType(), System.currentTimeMillis() + event.getDurationMillis());
    Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
}

@Subscribe
public void onTimerExpired(PlayerTimerExpiredDomainEvent event) {
    data.clearTimer(event.getTimerType());
    Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
}
```

### KOTH events

```java
@Subscribe public void onKothStarted(KothStartedDomainEvent event)  { /* add KothEntry, tickAll */ }
@Subscribe public void onKothCaptured(KothCapturedDomainEvent event) { /* remove KothEntry, tickAll */ }
@Subscribe public void onKothEnded(KothEndedDomainEvent event)       { /* remove KothEntry, tickAll */ }
```

---

## KOTH Capture Progress

`KothListener` calls `ScoreboardManager.updateKothCapture(uuid, kothName, remainingMs)` every second while a player is inside a capture zone. This data is stored in `PlayerBoardData` and used by `ScoreboardRenderer` to show a personalised countdown:

```java
long captureMs = koth.name.equals(data.getCapturingKothName())
        ? data.getCaptureRemainingMs() : 0;
```

When the player leaves the zone: `ScoreboardManager.clearKothCapture(uuid)`.

---

## Lifecycle

```java
// On player join (PlayerJoinEvent in ScoreboardListener):
scoreboardManager.createBoard(player);
  ├─ new PlayerBoardData()
  ├─ new PlayerBoard(player, "§e§lKRONOS HCF")   ← assigns Bukkit scoreboard
  └─ refreshStats(player.getUniqueId())           ← async initial stat load

// On player quit (PlayerQuitEvent in ScoreboardListener):
scoreboardManager.removeBoard(player);
  ├─ boards.remove(uuid)
  └─ cache.remove(uuid)
```

---

## Guide: How to Add a New Scoreboard Line

### Step 1 — Add a field to `PlayerBoardData`

```java
private volatile int myNewStat = 0;

public int getMyNewStat()         { return myNewStat; }
public void setMyNewStat(int v)   { this.myNewStat = v; }
```

### Step 2 — Populate the field

If it requires a database call (slow stat), add it to `ScoreboardManager.refreshStats`:

```java
myNewService.getValue(uuid).thenAccept(v -> {
    PlayerBoardData data = cache.get(uuid);
    if (data != null) data.setMyNewStat(v);
});
```

If it is event-driven (fast stat), subscribe to the relevant event in `ScoreboardManager` and set it directly:

```java
@Subscribe
public void onMyEvent(MyDomainEvent event) {
    PlayerBoardData data = cache.get(event.getPlayerUuid());
    if (data == null) return;
    data.setMyNewStat(event.getNewValue());
    Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
}
```

### Step 3 — Render the line in `ScoreboardRenderer`

Insert a `lines.add(...)` call in the `render` method at the desired position:

```java
lines.add(messages.format("scoreboard.mystat", "value", String.valueOf(data.getMyNewStat())));
```

### Step 4 — Add the template to `messages.yml`

```yaml
scoreboard:
  mystat: "&bMy Stat: &f{value}"
```

No restart is required if `MessagesConfig` hot-reload is implemented; otherwise reload with `/hcf reload`.
