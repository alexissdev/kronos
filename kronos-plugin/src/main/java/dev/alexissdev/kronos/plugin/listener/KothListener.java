package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.KothCaptureEvent;
import dev.alexissdev.kronos.api.event.KothStartEvent;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.koth.event.KothCapturedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothStartedDomainEvent;
import dev.alexissdev.kronos.koth.service.KothService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class KothListener implements Listener {

    private final KothService kothService;
    private final Plugin plugin;
    private final EventBus eventBus;
    private final MessagesConfig messages;

    private final ConcurrentHashMap<UUID, Long> captureProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> currentKothCapturing = new ConcurrentHashMap<>();

    @Inject
    public KothListener(KothService kothService, Plugin plugin, EventBus eventBus, MessagesConfig messages) {
        this.kothService = kothService;
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.messages = messages;
        this.eventBus.register(this);
        startCaptureTask();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hasMoved(event)) return;
        Player player = event.getPlayer();

        kothService.getActiveKoths().thenAccept(koths -> {
            boolean inAnyKoth = koths.stream().anyMatch(z ->
                    z.containsLocation(event.getTo().getWorld().getName(),
                            event.getTo().getX(), event.getTo().getZ()));

            if (!inAnyKoth) {
                captureProgress.remove(player.getUniqueId());
                currentKothCapturing.remove(player.getUniqueId());
            } else {
                koths.stream()
                        .filter(z -> z.containsLocation(event.getTo().getWorld().getName(),
                                event.getTo().getX(), event.getTo().getZ()))
                        .findFirst()
                        .ifPresent(z -> {
                            currentKothCapturing.put(player.getUniqueId(), z.getName());
                            captureProgress.putIfAbsent(player.getUniqueId(), System.currentTimeMillis());
                        });
            }
        });
    }

    @Subscribe
    public void onKothStarted(KothStartedDomainEvent event) {
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            KothCaptureEvent bukkitEvent = new KothCaptureEvent(event.getKothName(), event.getCaptorUuid());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            Player captor = Bukkit.getPlayer(event.getCaptorUuid());
            String captorName = captor != null ? captor.getName() : "Unknown";
            Bukkit.broadcastMessage(messages.format("koth.broadcast.captured",
                    "player", captorName, "name", event.getKothName()));
        });
    }

    private void startCaptureTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, String> entry : new ConcurrentHashMap<>(currentKothCapturing).entrySet()) {
                UUID playerUuid = entry.getKey();
                String kothName = entry.getValue();
                long startTime = captureProgress.getOrDefault(playerUuid, System.currentTimeMillis());
                long elapsed = System.currentTimeMillis() - startTime;

                kothService.getKoth(kothName).thenAccept(opt -> opt.ifPresent(zone -> {
                    if (!zone.isActive()) {
                        captureProgress.remove(playerUuid);
                        currentKothCapturing.remove(playerUuid);
                        return;
                    }
                    long requiredMs = (long) zone.getCaptureTimeSeconds() * 1000L;
                    if (elapsed >= requiredMs) {
                        captureProgress.remove(playerUuid);
                        currentKothCapturing.remove(playerUuid);
                        kothService.captureKoth(kothName, playerUuid)
                                .exceptionally(ex -> {
                                    plugin.getLogger().warning("Error al capturar KOTH " + kothName + ": " + ex.getMessage());
                                    return null;
                                });
                    }
                }));
            }
        }, 20L, 20L);
    }

    private boolean hasMoved(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}
