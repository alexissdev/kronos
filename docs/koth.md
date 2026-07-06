# kronos-koth

KOTH (King of the Hill) is a PvP event where players compete to stand inside a designated capture zone for a fixed period of time. The first player to accumulate the required capture time wins a crate reward.

---

## Concept

A KOTH zone has two distinct areas:

| Area | Purpose |
|---|---|
| **Claim zone** (`minX/minZ – maxX/maxZ`) | Full territory of the KOTH; used for build protection and map display |
| **Capture zone** (`captureMinX/captureMinZ – captureMaxX/captureMaxZ`) | Smaller inner area where a player must stand to accumulate capture time |

A player must remain **alone** inside the capture zone for `captureTimeSeconds` consecutive seconds to win. Being knocked out resets their progress. When the timer completes, `KothApplicationService.captureKoth(name, captorUuid)` is called and the zone becomes inactive.

---

## Domain Model — `KothZone`

`dev.alexissdev.kronos.koth.domain.KothZone`

| Field | Type | Description |
|---|---|---|
| `name` | `String` | Unique identifier for the KOTH zone |
| `world` | `String` | Bukkit world name |
| `minX` / `maxX` | `int` | Claim territory X bounds (block coordinates) |
| `minZ` / `maxZ` | `int` | Claim territory Z bounds |
| `captureMinX` / `captureMaxX` | `int` | Inner capture zone X bounds |
| `captureMinZ` / `captureMaxZ` | `int` | Inner capture zone Z bounds |
| `captureTimeSeconds` | `int` | Seconds to stand in capture zone to win |
| `rewardCrateType` | `CrateType` | Crate type awarded on capture (always `CrateType.KOTH`) |
| `active` | `boolean` | Mutable; `true` while the KOTH event is running |

Key methods:

```java
boolean containsLocation(String world, double x, double z)  // full territory check
boolean isInCaptureZone(String world, double x, double z)   // inner capture area check
void    setActive(boolean active)                           // called by the service
```

---

## Zone Creation Flow

Zone creation uses a two-phase interactive wand session.

### KothCreationSession

`dev.alexissdev.kronos.koth.creation.KothCreationSession`

| Phase | Steps |
|---|---|
| `CLAIM` | Admin right-clicks two blocks to define the full territory corners |
| `CAPTURE` | After both claim corners are set, admin right-clicks two blocks for the inner capture zone |

State progression:

```
new KothCreationSession(name, captureTimeSeconds)
  │
  ├─ [Phase.CLAIM]
  │    setClaimPos1(world, x, z)   ← first wand click
  │    setClaimPos2(x, z)          ← second wand click
  │    advanceToCapture()          ← called when isClaimComplete() == true
  │
  ├─ [Phase.CAPTURE]
  │    setCapturePos1(x, z)        ← third wand click
  │    setCapturePos2(x, z)        ← fourth wand click
  │
  └─ isComplete() == true → build()
        └─ new KothZone(name, world, min(...), max(...), ..., CrateType.KOTH)
```

`build()` automatically normalizes corners so min/max are always correct regardless of click order.

The session is managed by `KothCreationService` and `KothWandListener`.

---

## KOTH Lifecycle

```
Admin runs /koth create <name> <captureSeconds>
  └─ KothCreationSession created, stored per-admin UUID
       └─ Admin clicks 4 blocks with the KOTH wand
            └─ session.isComplete() → kothService.createKoth(session.build())
                 └─ KothRepository.save(zone)

Admin runs /koth start <name>
  └─ KothApplicationService.startKoth(name)
       ├─ zone.setActive(true)
       ├─ KothRepository.save(zone)
       └─ EventBus.post(KothStartedDomainEvent)
                 ↓
            ScoreboardManager registers KothEntry
            KothListener begins tracking capture progress

(Player stands in capture zone)
  └─ KothListener increments capture progress per tick
       └─ When remainingMs == 0:
            KothApplicationService.captureKoth(name, captorUuid)
              ├─ zone.setActive(false)
              ├─ KothRepository.save(zone)
              └─ EventBus.post(KothCapturedDomainEvent)
                        ↓
                   Crate reward delivered to captor
                   ScoreboardManager removes KothEntry

Admin runs /koth end <name>  (force-end without capture)
  └─ KothApplicationService.endKoth(name)
       ├─ zone.setActive(false)
       ├─ KothRepository.save(zone)
       └─ EventBus.post(KothEndedDomainEvent)
```

---

## Domain Events

| Event | Fields | Consumed by |
|---|---|---|
| `KothStartedDomainEvent` | `kothName`, `centerX`, `centerZ`, `captureTimeSeconds`, `zone` | `ScoreboardManager` (adds `KothEntry`) |
| `KothCapturedDomainEvent` | `kothName`, `captorUuid` | `ScoreboardManager` (removes entry), crate reward listener |
| `KothEndedDomainEvent` | `kothName` | `ScoreboardManager` (removes entry) |
| `KothDeletedDomainEvent` | `kothName` | Any listener cleaning up state |

`KothStartedDomainEvent` includes the center of the capture zone (`(captureMinX + captureMaxX) / 2`, `(captureMinZ + captureMaxZ) / 2`) so scoreboards and compasses can point to it.

---

## KothService Interface

`dev.alexissdev.kronos.koth.service.KothService`

| Method | Description |
|---|---|
| `startKoth(name)` | Activate an existing zone; posts `KothStartedDomainEvent` |
| `endKoth(name)` | Deactivate without capture; posts `KothEndedDomainEvent` |
| `captureKoth(name, captorUuid)` | Register capture; posts `KothCapturedDomainEvent` |
| `createKoth(zone)` | Persist a newly created zone |
| `deleteKoth(name)` | Permanently delete a zone; posts `KothDeletedDomainEvent` |
| `getKoth(name)` | Fetch by name |
| `getActiveKoths()` | All zones where `active == true` |
| `getAllKoths()` | All zones regardless of active state |

Additionally, `KothApplicationService.deactivateAll()` sets all active zones to inactive — called by `PluginDisableHandler` on server shutdown to keep database state consistent.

---

## Command Set (`/koth`)

| Subcommand | Permission | Description |
|---|---|---|
| `/koth create <name> <seconds>` | `kronos.admin` | Start an interactive creation session |
| `/koth delete <name>` | `kronos.admin` | Remove a KOTH zone permanently |
| `/koth start <name>` | `kronos.admin` | Begin the KOTH event |
| `/koth end <name>` | `kronos.admin` | Force-end a running KOTH event |
| `/koth list` | `kronos.admin` | List all zones with active status |
| `/koth info <name>` | `kronos.admin` | Display zone details (coordinates, capture time) |

---

## Guide: How to Create a New KOTH via Commands

1. Stand in-game as an admin.
2. Run `/koth create Hill 300` (name = "Hill", 5-minute capture time).
3. A KOTH wand (stick) is given to you. You will be informed which phase you are in.
4. **Phase CLAIM:** Right-click two blocks that mark the diagonal corners of the full KOTH territory.
5. After the second click, the session advances automatically to **Phase CAPTURE**.
6. **Phase CAPTURE:** Right-click two blocks that mark the diagonal corners of the inner capture zone (must be inside the claim territory).
7. After the fourth click, the zone is saved to MongoDB. You receive a confirmation message.
8. To start the event: `/koth start Hill`
9. Players now see the KOTH on their scoreboards and can compete to capture it.

---

## Guice Module — `KothModule`

Binds `KothApplicationService` as singleton and `KothRepository` to its MongoDB implementation.
