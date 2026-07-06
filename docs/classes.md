# kronos-classes

The classes module implements HCF combat classes (kits). Each class grants passive and active abilities to the player based on the helmet they are wearing.

---

## What are HCF Classes?

In HCF, players choose a "class" by equipping a specific helmet type. The class determines:

- **Passive abilities**: applied automatically while the kit is active (e.g. speed bursts, support auras).
- **Active ability**: triggered on demand (e.g. right-clicking with a specific item); subject to a `CLASS_COOLDOWN` timer.

The active kit is stored on the player's `HCFPlayer` profile in MongoDB and kept in sync by `ClassListener` whenever the player changes helmets.

---

## KitType Enum

`dev.alexissdev.kronos.classes.domain.KitType`

| Value | Helmet | Passive | Active |
|---|---|---|---|
| `ARCHER` | Leather | Shoots arrows faster; applies Slowness on melee hit | Brief damage boost |
| `BARD` | Gold | Grants Speed and Regeneration to nearby faction members (15-block radius) | Amplified speed aura for allies |
| `ROGUE` | Chain | Gains Speed on melee hit | Brief Invisibility |
| `MINER` | Iron | Haste passive while breaking blocks | Increased mining speed burst |
| `KNIGHT` | Diamond (special) | Resistance on melee hit | Knockback repel for nearby players |
| `DIAMOND` | Any / default | No special abilities | None |

`DIAMOND` is the default kit assigned to new players and to players without a recognized kit helmet. It has no special abilities.

---

## Class Detection

The active kit is determined by the helmet slot. `ClassListener` listens to:

- `PlayerArmorStandsManipulateEvent` — catches some armor changes
- `InventoryClickEvent` — catches helmet slot changes in the inventory
- `PlayerJoinEvent` — reads `HCFPlayer.activeKit` from profile to initialize the cache

When a helmet change is detected:

```java
KitType newKit = resolveKitFromHelmet(player.getInventory().getHelmet());
kitService.updateActiveKit(player.getUniqueId(), newKit);
```

`updateActiveKit` saves the new `KitType` to the player's MongoDB profile via `PlayerRepository`.

---

## Applying and Removing Effects

`ClassListener` continuously applies passive effects using `runTaskTimerAsynchronously` (or `runTaskTimer` for Bukkit effects):

- **Bard**: every 2 seconds, checks all online players within 15 blocks of each active Bard and applies `PotionEffect(Speed)` + `PotionEffect(Regeneration)` with a 3-second duration (so effects refresh before expiry).
- **Archer / Rogue / Miner / Knight**: passive effects are triggered on specific Bukkit events (e.g. `EntityDamageByEntityEvent`, `BlockBreakEvent`).

When a player's kit changes to `DIAMOND` or they quit, any active periodic buffs stop.

---

## Active Abilities and CLASS_COOLDOWN

Active abilities are triggered by right-clicking a designated item (e.g. a blaze rod for Bard). The `ClassListener` handles `PlayerInteractEvent`:

1. Verify the player's active kit matches the ability's kit.
2. Call `kitService.isClassAbilityOnCooldown(uuid)`.
3. If on cooldown, send a "Ability on cooldown (Xs)" message.
4. If not on cooldown, apply the effect and call `kitService.activateClassAbility(uuid, kitType)`.

`activateClassAbility` starts a `CLASS_COOLDOWN` timer on `TimerApplicationService`, which also appears on the player's scoreboard.

---

## KitService Interface

`dev.alexissdev.kronos.classes.service.KitService`

| Method | Parameters | Return | Description |
|---|---|---|---|
| `detectKit` | `UUID playerUuid` | `CompletableFuture<Optional<KitType>>` | Read active kit from MongoDB profile |
| `activateClassAbility` | `UUID playerUuid, KitType kitType` | `CompletableFuture<Void>` | Start `CLASS_COOLDOWN` timer |
| `isClassAbilityOnCooldown` | `UUID playerUuid` | `CompletableFuture<Boolean>` | Check if `CLASS_COOLDOWN` timer is active |
| `updateActiveKit` | `UUID playerUuid, KitType kitType` | `CompletableFuture<Void>` | Save new kit to MongoDB profile |

---

## Guide: How to Add a New Class

### Step 1 — Add a KitType value

In `dev.alexissdev.kronos.classes.domain.KitType`, add the new enum constant with Javadoc:

```java
/**
 * Pyromancer class: ignites nearby enemies on melee hit.
 * Activated with a netherbrick helmet.
 */
PYROMANCER,
```

### Step 2 — Map the helmet to the kit

In `ClassListener`, update the `resolveKitFromHelmet` method to recognize the new helmet material:

```java
case NETHER_BRICK:  // or whichever Material maps to this class
    return KitType.PYROMANCER;
```

### Step 3 — Implement the passive effect

In `ClassListener.onEntityDamageByEntity` (or a new listener method), add a branch for the new kit:

```java
if (activeKit == KitType.PYROMANCER && event.getDamager() instanceof Player) {
    Entity victim = event.getEntity();
    victim.setFireTicks(60); // 3 seconds of fire
}
```

### Step 4 — Implement the active ability

In `ClassListener.onPlayerInteract` (right-click handler), add:

```java
case PYROMANCER:
    // Apply ring-of-fire effect
    player.getNearbyEntities(5, 5, 5).forEach(e -> e.setFireTicks(100));
    break;
```

Then call `kitService.activateClassAbility(player.getUniqueId(), KitType.PYROMANCER)` to start the cooldown.

### Step 5 — Add display name and scoreboard key

In `messages.yml`:

```yaml
kit:
  pyromancer:
    name: "&cPyromancer"
scoreboard:
  timer:
    class: "&6Class &7{time}"  # already exists; no change needed
```

### Step 6 — Test

Equip the new helmet in-game and verify the passive triggers, the active ability works, and the `CLASS_COOLDOWN` timer appears on the scoreboard.
