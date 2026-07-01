# HCF Plugin API

API pública del plugin Kronos HCF para inter-plugin communication via Bukkit ServicesManager.

## Dependencia

Agrega `HCFPlugin` como dependencia en tu `plugin.yml`:

```yaml
depend: [HCFPlugin]
```

En tu `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly(files("libs/kronos-api.jar"))
}
```

## Obtener la API

```java
RegisteredServiceProvider<HCFApi> provider =
    Bukkit.getServicesManager().getRegistration(HCFApi.class);

if (provider != null) {
    HCFApi hcf = provider.getProvider();
    // usar la api...
}
```

## FactionApi

```java
HCFApi hcf = provider.getProvider();

// Obtener facción de un jugador
hcf.factions().getByPlayer(player.getUniqueId())
    .ifPresent(f -> player.sendMessage("Facción: " + f.getName()));

// Verificar si están en facción
boolean inFaction = hcf.factions().isInFaction(player.getUniqueId());

// Top facciones
List<FactionSnapshot> top = hcf.factions().getTopFactions(10);

// Verificar alianza
boolean allies = hcf.factions().areAllies(factionIdA, factionIdB);
```

## PlayerDataApi

```java
// Obtener stats del jugador
hcf.players().getPlayer(uuid).ifPresent(p -> {
    System.out.println("Kills: " + p.getKills());
    System.out.println("Deaths: " + p.getDeaths());
    System.out.println("Balance: " + p.getBalance());
});

// Métodos directos
int kills = hcf.players().getKills(uuid);
double balance = hcf.players().getBalance(uuid);
boolean online = hcf.players().isOnline(uuid);
```

## TimerApi

```java
// Verificar combat tag
if (hcf.timers().hasCombatTag(player.getUniqueId())) {
    player.sendMessage("¡Estás en combate!");
}

// Tiempo restante de pvp timer
OptionalLong remaining = hcf.timers().getRemainingMillis(uuid, TimerType.PVP_TIMER);
remaining.ifPresent(ms -> player.sendMessage("PvP Timer: " + (ms / 1000) + "s"));

// Verificar cooldown de enderpearl
boolean cooldown = hcf.timers().hasEnderpearlCooldown(uuid);
```

## ClaimApi

```java
// Tipo de territorio en una ubicación
ClaimType type = hcf.claims().getClaimTypeAt(player.getLocation());

// Verificar si es zona segura
if (hcf.claims().isSafeZone(player.getLocation())) {
    player.sendMessage("Estás en spawn.");
}

// Obtener claim en un chunk
hcf.claims().getClaimAt(world, chunkX, chunkZ)
    .ifPresent(c -> System.out.println("Claim: " + c.getFactionId()));
```

## KothApi

```java
// KOTHs activos
List<KothSnapshot> active = hcf.koth().getActiveKoths();

// Estado de un KOTH específico
boolean isActive = hcf.koth().isKothActive("Castle");

hcf.koth().getKoth("Castle").ifPresent(k -> {
    System.out.println("KOTH activo: " + k.isActive());
    System.out.println("Mundo: " + k.getWorld());
});
```

## Custom Events

Los siguientes eventos son disparados por el plugin y pueden escucharse normalmente:

| Evento                   | Cancellable | Descripción                        |
|--------------------------|-------------|------------------------------------|
| `FactionCreateEvent`     | ✅           | Se crea una facción                |
| `FactionDisbandEvent`    | ❌           | Se disuelve una facción            |
| `FactionClaimEvent`      | ✅           | Una facción reclama territorio     |
| `PlayerJoinFactionEvent` | ✅           | Un jugador acepta una invitación   |
| `PlayerLeaveFactionEvent`| ❌           | Un jugador sale/es expulsado       |
| `PlayerCombatTagEvent`   | ✅           | Un jugador entra en combat tag     |
| `PlayerTimerExpireEvent` | ❌           | Un timer de jugador expira         |
| `KothStartEvent`         | ✅           | Inicia un KOTH                     |
| `KothCaptureEvent`       | ❌           | Un jugador captura un KOTH         |

```java
@EventHandler
public void onCombatTag(PlayerCombatTagEvent event) {
    if (event.isCancelled()) return;
    Player player = Bukkit.getPlayer(event.getTagged());
    if (player != null) {
        player.sendMessage("§c¡Entraste en combate!");
    }
}
```
