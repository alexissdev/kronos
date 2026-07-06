# kronos-claims

The claims module implements chunk-based territory ownership. Every 16×16 chunk can belong to a faction, a system zone (SafeZone, WarZone, Road), or be unclaimed (Wilderness).

---

## Domain Model — `Claim`

`dev.alexissdev.kronos.claims.domain.Claim`

| Field | Type | Description |
|---|---|---|
| `id` | `String` | UUID-as-string; generated at creation |
| `factionId` | `String` | Owner faction ID; `null` for system claims |
| `type` | `ClaimType` | Zone category |
| `world` | `String` | Bukkit world name |
| `minChunkX` / `maxChunkX` | `int` | Rectangular chunk bounds on the X axis |
| `minChunkZ` / `maxChunkZ` | `int` | Rectangular chunk bounds on the Z axis |

The entity is **immutable** after construction. To change ownership or size, delete the old claim and create a new one.

Useful methods:

```java
boolean containsChunk(int chunkX, int chunkZ)  // used in movement listeners
int     getChunkCount()                         // (maxX - minX + 1) * (maxZ - minZ + 1)
```

---

## ClaimType Enum

`dev.alexissdev.kronos.claims.domain.ClaimType`

| Value | System claim? | PvP allowed? | Build protected? | Description |
|---|---|---|---|---|
| `FACTION` | No | No (unless enemy/raidable) | Yes | Player faction territory |
| `SAFEZONE` | Yes | No | Yes | Protected area (typically spawn vicinity) |
| `WARZONE` | Yes | Yes | Yes | Always-combat zone without faction ownership |
| `ROAD` | Yes | No | Yes | Server-managed separator between territories |
| `WILDERNESS` | Yes | Yes | No | Unclaimed land; free to build and fight |
| `KOTH` | No | Yes | Yes | Active KOTH event territory |
| `CITADEL` | No | Yes | Yes | High-value special event zone |

Helper predicates:

```java
boolean isSystemClaim()        // SAFEZONE, WARZONE, ROAD, WILDERNESS
boolean allowsPvp()            // WARZONE, KOTH, CITADEL, WILDERNESS
boolean isProtectedFromBuild() // everything except WILDERNESS
```

---

## Territory Operations

### Claiming

`ClaimApplicationService.claim(factionId, actorUuid, world, minCX, minCZ, maxCX, maxCZ)`

1. Loads the faction and verifies the actor has at least `CAPTAIN` role.
2. Checks every chunk in the rectangle in parallel using `ClaimRepository.findByChunk`. If any chunk is already claimed, throws `ClaimConflictException`.
3. Creates a new `Claim` of type `FACTION` and persists it.
4. Posts `FactionClaimedDomainEvent` so `ClaimListener` can add it to its in-memory cache.

### Unclaiming

`ClaimApplicationService.unclaim(factionId, actorUuid, world, chunkX, chunkZ)`

1. Loads the claim at the given chunk.
2. Verifies `claim.getFactionId().equals(factionId)` — only the owning faction can unclaim.
3. Deletes from the repository.

### Unclaiming All

`ClaimApplicationService.unclaimAll(factionId)`

Delegates directly to `ClaimRepository.deleteByFaction(factionId)`. Called automatically when `FactionDisbandedDomainEvent` is received by a listener (all faction territory is released on disband).

### Overclaiming

`ClaimApplicationService.overclaim(factionId, actorUuid, world, chunkX, chunkZ)`

Overclaiming allows a faction to capture enemy territory without the enemy's consent.

**Requirements:**

1. Actor must have `CAPTAIN` or higher role in the attacking faction.
2. The chunk must contain an existing `FACTION` claim belonging to a different faction.
3. The defending faction must either:
   - Be listed as an **enemy** of the attacking faction, **or**
   - Have `raidable = true` (DTK reached 0)

**Execution (`doOverclaim`):**

1. Delete the existing claim.
2. Create a new `FACTION` claim with identical chunk bounds, owned by the attacking faction.
3. Post `FactionClaimedDomainEvent` to update the `ClaimListener` cache.

---

## Conflict Detection

Conflict detection is chunk-level. The `ClaimRepository.findByChunk(world, chunkX, chunkZ)` call returns the claim covering that chunk (if any). The `containsChunk(chunkX, chunkZ)` method on `Claim` is used by the `ClaimListener` in-memory cache for fast O(1) lookups during player movement without database I/O.

When performing bulk claiming of a rectangle, all chunk checks run in parallel:

```java
for (int x = minChunkX; x <= maxChunkX; x++) {
    for (int z = minChunkZ; z <= maxChunkZ; z++) {
        checks.add(claimRepository.findByChunk(world, x, z));
    }
}
CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
    .thenCompose(v -> {
        boolean conflict = checks.stream().anyMatch(f -> f.join().isPresent());
        if (conflict) throw new ClaimConflictException("...");
        // proceed with save
    });
```

---

## Integration with Factions

The claims module depends on `kronos-factions` for:

- Permission checks (actor's `FactionMember.role` must be `CAPTAIN+`).
- Overclaim validation (checking `Faction.isEnemy(factionId)` and `Faction.isRaidable()`).

Factions do **not** depend on claims — the coupling is unidirectional. Cross-module coordination happens through:

- `FactionClaimedDomainEvent` — posted by claims, consumed by `ClaimListener` (cache invalidation)
- `FactionDisbandedDomainEvent` — posted by factions, consumed by a claims listener that calls `unclaimAll`

---

## ClaimService Interface

`dev.alexissdev.kronos.claims.service.ClaimService`

| Method | Description |
|---|---|
| `claim(...)` | Claim a rectangular area |
| `unclaim(...)` | Release a single chunk |
| `unclaimAll(factionId)` | Release all faction territory |
| `getClaimAt(world, chunkX, chunkZ)` | Fetch claim at a chunk |
| `getClaimTypeAt(world, chunkX, chunkZ)` | Returns `WILDERNESS` if no claim found |
| `getFactionClaims(factionId)` | All claims owned by a faction |
| `getAllClaims()` | All claims; used during startup to populate the in-memory cache |
| `overclaim(...)` | Conquer an enemy chunk |

---

## Exceptions

| Exception | When thrown |
|---|---|
| `ClaimConflictException` | A chunk is already claimed, or overclaim conditions are not met |
| `FactionPermissionException` | Actor's role is below the required minimum |
| `FactionNotFoundException` | The provided `factionId` does not exist |
