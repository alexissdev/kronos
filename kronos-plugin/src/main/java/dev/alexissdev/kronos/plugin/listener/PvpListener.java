package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.SotwService;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.spawn.SpawnApplicationService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import dev.alexissdev.kronos.factions.domain.Faction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener central del sistema de PvP del plugin HCF.
 *
 * <p>Gestiona todas las mecánicas relacionadas con el combate jugador vs. jugador:
 * <ul>
 *   <li><strong>Combat tag</strong>: marca a los jugadores como en combate al recibir daño de otro jugador,
 *       impidiendo que se desconecten sin consecuencias.</li>
 *   <li><strong>PvP timer</strong>: protección temporal para jugadores nuevos que impide que sean atacados
 *       y que ellos ataquen a otros.</li>
 *   <li><strong>SOTW</strong>: durante el periodo Start of the World, todo el PvP queda bloqueado.</li>
 *   <li><strong>Spawn protection</strong>: cancela el daño cuando cualquiera de los dos jugadores está en spawn.</li>
 *   <li><strong>Enderpearl cooldown</strong>: aplica un cooldown al lanzar perlas de éter en combate
 *       y bloquea su uso en spawn. Además, valida el destino de la teleportación por perla según el
 *       tipo de territorio (reclamación protegida, saqueable o territorio aliado).</li>
 *   <li><strong>Gapple cooldown</strong>: impide consumir manzanas doradas si el cooldown está activo.</li>
 *   <li><strong>Deathban</strong>: al morir sin vidas restantes, el jugador es expulsado y se le aplica
 *       un ban temporal configurado.</li>
 *   <li><strong>Kill y death tracking</strong>: registra kills y descuenta vidas al morir.</li>
 *   <li><strong>DTK (Deaths to Kill)</strong>: notifica al servicio de facciones cuando un miembro muere
 *       a manos de un jugador de otra facción para decrementar el contador DTK.</li>
 * </ul>
 */
@Singleton
public class PvpListener implements Listener {

    private final TimerApplicationService timerService;
    private final PlayerService playerService;
    private final FactionService factionService;
    private final ClaimService claimService;
    private final DeathbanRepository deathbanRepository;
    private final SpawnApplicationService spawnService;
    private final MessagesConfig messages;
    private final Plugin plugin;
    private final SotwService sotwService;
    private final long deathbanDurationSeconds;
    private final long enderpearlCooldownMs;
    private final long gappleCooldownMs;

    /** UUIDs de jugadores que acaban de teletransportarse con perla, usados para cancelar el daño de caída. */
    private final Set<UUID> recentlyPearled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Crea el listener de PvP con todas sus dependencias inyectadas, incluyendo parámetros
     * de configuración anotados con {@code @Named}.
     *
     * @param timerService           servicio de timers (combat tag, PvP timer, enderpearl, gapple, home)
     * @param playerService          servicio para gestionar datos y vidas de los jugadores
     * @param factionService         servicio de facciones para calcular mensajes de muerte y DTK
     * @param claimService           servicio de reclamaciones para validar destinos de perlas
     * @param deathbanRepository     repositorio para aplicar y consultar deathbans
     * @param spawnService           servicio de spawn para verificar si un jugador está en zona protegida
     * @param messages               configuración de mensajes localizada
     * @param plugin                 instancia del plugin principal para programar tareas
     * @param sotwService            servicio que indica si el período SOTW está activo
     * @param deathbanDurationSeconds duración del deathban en segundos (de {@code config.yml})
     * @param enderpearlCooldownMs   cooldown de la perla de éter en milisegundos
     * @param gappleCooldownMs       cooldown de la manzana dorada en milisegundos
     */
    @Inject
    public PvpListener(TimerApplicationService timerService,
                       PlayerService playerService,
                       FactionService factionService,
                       ClaimService claimService,
                       DeathbanRepository deathbanRepository,
                       SpawnApplicationService spawnService,
                       MessagesConfig messages,
                       Plugin plugin,
                       SotwService sotwService,
                       @Named("hcf.deathban-seconds")   long deathbanDurationSeconds,
                       @Named("enderpearl.cooldown-ms") long enderpearlCooldownMs,
                       @Named("gapple.cooldown-ms")     long gappleCooldownMs) {
        this.timerService = timerService;
        this.playerService = playerService;
        this.factionService = factionService;
        this.claimService = claimService;
        this.deathbanRepository = deathbanRepository;
        this.spawnService = spawnService;
        this.messages = messages;
        this.plugin = plugin;
        this.sotwService = sotwService;
        this.deathbanDurationSeconds = deathbanDurationSeconds;
        this.enderpearlCooldownMs = enderpearlCooldownMs;
        this.gappleCooldownMs = gappleCooldownMs;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player victim   = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        if (sotwService.isSotwActive()) {
            event.setCancelled(true);
            attacker.sendMessage(messages.get("sotw.pvp-blocked"));
            return;
        }

        boolean victimInSpawn   = spawnService.getZone().map(z -> z.contains(victim.getLocation())).orElse(false);
        boolean attackerInSpawn = spawnService.getZone().map(z -> z.contains(attacker.getLocation())).orElse(false);
        if (victimInSpawn || attackerInSpawn) {
            event.setCancelled(true);
            attacker.sendMessage(messages.get("pvp.spawn-protected"));
            return;
        }

        if (timerService.hasActiveTimerSync(victim.getUniqueId(), TimerType.PVP_TIMER)) {
            event.setCancelled(true);
            attacker.sendMessage(messages.format("pvp.target-has-pvp-timer", "player", victim.getName()));
            return;
        }

        if (timerService.hasActiveTimerSync(attacker.getUniqueId(), TimerType.PVP_TIMER)) {
            event.setCancelled(true);
            attacker.sendMessage(messages.get("pvp.attacker-has-pvp-timer"));
            return;
        }

        timerService.tagForCombat(victim.getUniqueId(), attacker.getUniqueId());

        // Cancel pending home teleports for both players
        cancelHomeTeleport(victim);
        cancelHomeTeleport(attacker);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnderpearlThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        UUID uuid = player.getUniqueId();

        if (spawnService.getZone().map(z -> z.contains(player.getLocation())).orElse(false)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL)));
            player.sendMessage(messages.get("pvp.pearl-spawn-blocked"));
            return;
        }

        if (timerService.hasActiveTimerSync(uuid, TimerType.ENDERPEARL)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL)));
            timerService.getRemainingMillis(uuid, TimerType.ENDERPEARL).thenAccept(opt ->
                    opt.ifPresent(ms -> player.sendMessage(
                            messages.format("timers.enderpearl.on-cooldown", "remaining", formatMs(ms)))));
            return;
        }

        if (timerService.hasActiveTimerSync(uuid, TimerType.COMBAT_TAG)) {
            timerService.startEnderpearlCooldown(uuid, enderpearlCooldownMs);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        Location destination = event.getTo();

        // Cancel immediately; we'll re-teleport manually if allowed
        event.setCancelled(true);

        claimService.getClaimAt(
                destination.getWorld().getName(),
                destination.getChunk().getX(),
                destination.getChunk().getZ()
        ).thenCompose(claimOpt -> {
            if (claimOpt.isEmpty() || !claimOpt.get().getType().isProtectedFromBuild()) {
                allowPearlTeleport(player, destination);
                return CompletableFuture.completedFuture(null);
            }
            Claim claim = claimOpt.get();
            String claimFactionId = claim.getFactionId();
            return factionService.getByPlayer(player.getUniqueId()).thenCompose(playerFactionOpt -> {
                String playerFactionId = playerFactionOpt.map(Faction::getId).orElse(null);
                boolean own  = claimFactionId != null && claimFactionId.equals(playerFactionId);
                boolean ally = playerFactionOpt.map(f -> f.isAlly(claimFactionId)).orElse(false);
                if (own || ally) {
                    allowPearlTeleport(player, destination);
                    return CompletableFuture.completedFuture(null);
                }
                if (claimFactionId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                        player.sendMessage(messages.get("pvp.pearl-claim-blocked"));
                    });
                    return CompletableFuture.completedFuture(null);
                }
                return factionService.getById(claimFactionId).thenAccept(ownerOpt -> {
                    boolean raidable = ownerOpt.map(Faction::isRaidable).orElse(false);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (raidable) {
                            pearlTeleport(player, destination);
                        } else {
                            player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                            player.sendMessage(messages.get("pvp.pearl-claim-blocked"));
                        }
                    });
                });
            });
        });
    }

    private void allowPearlTeleport(Player player, Location destination) {
        Bukkit.getScheduler().runTask(plugin, () -> pearlTeleport(player, destination));
    }

    private void pearlTeleport(Player player, Location destination) {
        recentlyPearled.add(player.getUniqueId());
        player.teleport(destination);
        // Remove flag after 40 ticks to cover any delayed fall damage
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> recentlyPearled.remove(player.getUniqueId()), 40L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (recentlyPearled.remove(((Player) event.getEntity()).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGappleConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.GOLDEN_APPLE) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (timerService.hasActiveTimerSync(uuid, TimerType.GAPPLE)) {
            event.setCancelled(true);
            timerService.getRemainingMillis(uuid, TimerType.GAPPLE).thenAccept(opt ->
                    opt.ifPresent(ms -> player.sendMessage(
                            messages.format("timers.gapple.on-cooldown", "remaining", formatMs(ms)))));
            return;
        }

        timerService.startGappleCooldown(uuid, gappleCooldownMs);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        UUID victimUuid = victim.getUniqueId();

        timerService.cancelTimer(victimUuid, TimerType.COMBAT_TAG);
        timerService.cancelTimer(victimUuid, TimerType.PVP_TIMER);
        timerService.cancelTimer(victimUuid, TimerType.HOME);

        // Override death message with faction-aware format
        event.setDeathMessage(null);
        final String victimName = victim.getName();
        final UUID killerUuid = killer != null ? killer.getUniqueId() : null;
        final String killerName = killer != null ? killer.getName() : null;

        CompletableFuture<Optional<Faction>> vFactionFuture =
                factionService.getByPlayer(victimUuid);
        CompletableFuture<Optional<Faction>> kFactionFuture =
                killerUuid != null ? factionService.getByPlayer(killerUuid)
                        : CompletableFuture.completedFuture(Optional.empty());

        vFactionFuture.thenCombine(kFactionFuture, (vFact, kFact) -> {
            String vTag = vFact.map(f -> ChatColor.GRAY + "[" + ChatColor.YELLOW + f.getName() + ChatColor.GRAY + "] ").orElse("");
            String kTag = kFact.map(f -> ChatColor.GRAY + "[" + ChatColor.YELLOW + f.getName() + ChatColor.GRAY + "] ").orElse("");

            // Decrement DTK only when killed by a player from a different faction
            vFact.ifPresent(victimFaction -> {
                boolean sameTeam = kFact.map(kf -> kf.getId().equals(victimFaction.getId())).orElse(false);
                if (!sameTeam && killerUuid != null) {
                    factionService.notifyMemberDeath(victimFaction.getId(), victimUuid);
                }
            });

            if (killerName != null) {
                return messages.format("pvp.death-killed",
                        "victim_tag", vTag, "victim", victimName,
                        "killer_tag", kTag, "killer", killerName);
            }
            return messages.format("pvp.death-natural", "victim_tag", vTag, "victim", victimName);
        }).thenAccept(msg -> Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg)));

        playerService.decrementLives(victimUuid)
                .thenCompose(remainingLives -> {
                    // Run recordKill AFTER decrement so saves don't overwrite each other
                    CompletableFuture<Void> killRecord = killerUuid != null
                            ? playerService.recordKill(killerUuid, victimUuid)
                            : CompletableFuture.completedFuture(null);
                    return killRecord.thenApply(v -> remainingLives);
                })
                .thenAccept(remainingLives -> {
                    if (remainingLives <= 0) {
                        String duration = formatSeconds(deathbanDurationSeconds);
                        String broadcastMsg = messages.format("deathban.broadcast",
                                "player", victimName, "time", duration);
                        deathbanRepository.setDeathban(victimUuid, deathbanDurationSeconds).thenRun(() ->
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    for (Player online : Bukkit.getOnlinePlayers()) {
                                        online.sendMessage(broadcastMsg);
                                    }
                                    Player p = Bukkit.getPlayer(victimUuid);
                                    if (p != null) {
                                        p.kickPlayer(messages.format("deathban.kick-message",
                                                "time", duration));
                                    }
                                }));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player p = Bukkit.getPlayer(victimUuid);
                            if (p != null) {
                                p.sendMessage(messages.format("deathban.lives-remaining", "lives", remainingLives));
                            }
                        });
                    }
                });
    }

    private void cancelHomeTeleport(Player player) {
        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.HOME)) {
            timerService.cancelTimer(player.getUniqueId(), TimerType.HOME);
            player.sendMessage(messages.get("faction.cmd.home-cancelled"));
        }
    }

    private static String formatMs(long ms) {
        long totalSecs = ms / 1000;
        long minutes = totalSecs / 60;
        long seconds = totalSecs % 60;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private static String formatSeconds(long totalSeconds) {
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
