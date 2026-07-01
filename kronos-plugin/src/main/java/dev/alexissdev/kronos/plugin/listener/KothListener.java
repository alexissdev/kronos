package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.KothCaptureEvent;
import dev.alexissdev.kronos.api.event.KothStartEvent;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.event.KothCapturedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothDeletedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothEndedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothStartedDomainEvent;
import dev.alexissdev.kronos.koth.service.KothService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class KothListener implements Listener {

    private final KothService kothService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    // All registered KOTHs — used for zone enter/leave messages regardless of active state
    private final ConcurrentHashMap<String, KothZone> allKothCache    = new ConcurrentHashMap<>();
    // Only active KOTHs — used for capture logic
    private final ConcurrentHashMap<String, KothZone> activeKothCache = new ConcurrentHashMap<>();

    // Per-player outer zone tracking (for enter/leave messages)
    private final ConcurrentHashMap<UUID, String> playerOuterZone = new ConcurrentHashMap<>();

    // Per-player capture state
    private final ConcurrentHashMap<UUID, String> capturingKoth = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>   captureStart  = new ConcurrentHashMap<>();

    @Inject
    public KothListener(KothService kothService, Plugin plugin, EventBus eventBus, MessagesConfig messages) {
        this.kothService = kothService;
        this.plugin      = plugin;
        this.messages    = messages;
        eventBus.register(this);

        kothService.getAllKoths().thenAccept(zones -> {
            for (KothZone z : zones) {
                allKothCache.put(z.getName(), z);
                if (z.isActive()) activeKothCache.put(z.getName(), z);
            }
        });

        startCaptureTask();
    }

    // ── Domain events → update caches ─────────────────────────────────────

    @Subscribe
    public void onKothStarted(KothStartedDomainEvent event) {
        KothZone zone = event.getZone();
        allKothCache.put(zone.getName(), zone);
        activeKothCache.put(zone.getName(), zone);

        Bukkit.getScheduler().runTask(plugin, () -> {
            KothStartEvent bukkitEvent = new KothStartEvent(event.getKothName());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            if (!bukkitEvent.isCancelled()) {
                Bukkit.broadcastMessage(messages.format("koth.broadcast.started", "name", event.getKothName()));
            }
        });
    }

    @Subscribe
    public void onKothCaptured(KothCapturedDomainEvent event) {
        activeKothCache.remove(event.getKothName());
        evictCapturingPlayers(event.getKothName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            KothCaptureEvent bukkitEvent = new KothCaptureEvent(event.getKothName(), event.getCaptorUuid());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            Player captor = Bukkit.getPlayer(event.getCaptorUuid());
            String captorName = captor != null ? captor.getName() : "Unknown";
            Bukkit.broadcastMessage(messages.format("koth.broadcast.captured",
                    "player", captorName, "name", event.getKothName()));
        });
    }

    @Subscribe
    public void onKothEnded(KothEndedDomainEvent event) {
        activeKothCache.remove(event.getKothName());
        evictCapturingPlayers(event.getKothName());
    }

    @Subscribe
    public void onKothDeleted(KothDeletedDomainEvent event) {
        allKothCache.remove(event.getKothName());
        activeKothCache.remove(event.getKothName());
        evictCapturingPlayers(event.getKothName());
        playerOuterZone.entrySet().removeIf(e -> event.getKothName().equals(e.getValue()));
    }

    // ── Player movement ───────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hasCrossedBlock(event)) return;
        if (allKothCache.isEmpty()) return;

        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();
        String world  = event.getTo().getWorld().getName();
        double x      = event.getTo().getX();
        double z      = event.getTo().getZ();

        // -- Outer zone: enter/leave messages (all KOTHs, active or not) --
        KothZone newOuter = null;
        for (KothZone kz : allKothCache.values()) {
            if (kz.containsLocation(world, x, z)) { newOuter = kz; break; }
        }
        String prevOuterName = playerOuterZone.get(uuid);
        String newOuterName  = newOuter != null ? newOuter.getName() : null;

        if (!Objects.equals(prevOuterName, newOuterName)) {
            if (newOuterName != null) {
                player.sendMessage(messages.format("koth.entered-zone", "name", newOuterName));
                playerOuterZone.put(uuid, newOuterName);
            } else {
                player.sendMessage(messages.format("koth.left-zone", "name", prevOuterName));
                playerOuterZone.remove(uuid);
            }
        }

        // -- Capture zone: only active KOTHs --
        KothZone captureZone = null;
        for (KothZone kz : activeKothCache.values()) {
            if (kz.isInCaptureZone(world, x, z)) { captureZone = kz; break; }
        }

        if (captureZone != null) {
            capturingKoth.put(uuid, captureZone.getName());
            captureStart.putIfAbsent(uuid, System.currentTimeMillis());
        } else {
            capturingKoth.remove(uuid);
            captureStart.remove(uuid);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerOuterZone.remove(uuid);
        capturingKoth.remove(uuid);
        captureStart.remove(uuid);
    }

    // ── Capture tick ──────────────────────────────────────────────────────

    private void startCaptureTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, String> entry : new ConcurrentHashMap<>(capturingKoth).entrySet()) {
                UUID   playerUuid = entry.getKey();
                String kothName   = entry.getValue();

                KothZone zone = activeKothCache.get(kothName);
                if (zone == null || !zone.isActive()) {
                    capturingKoth.remove(playerUuid);
                    captureStart.remove(playerUuid);
                    continue;
                }

                long startTime  = captureStart.getOrDefault(playerUuid, System.currentTimeMillis());
                long elapsed    = System.currentTimeMillis() - startTime;
                long requiredMs = (long) zone.getCaptureTimeSeconds() * 1000L;

                if (elapsed >= requiredMs) {
                    capturingKoth.remove(playerUuid);
                    captureStart.remove(playerUuid);
                    kothService.captureKoth(kothName, playerUuid)
                            .exceptionally(ex -> {
                                plugin.getLogger().warning("Error al capturar KOTH " + kothName + ": " + ex.getMessage());
                                return null;
                            });
                }
            }
        }, 20L, 20L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void evictCapturingPlayers(String kothName) {
        capturingKoth.entrySet().removeIf(e -> kothName.equals(e.getValue()));
        captureStart.entrySet().removeIf(e -> !capturingKoth.containsKey(e.getKey()));
    }

    private boolean hasCrossedBlock(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}
