package dev.alexissdev.kronos.timers;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.timers.domain.Timer;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.timers.event.PlayerCombatTaggedDomainEvent;
import dev.alexissdev.kronos.timers.event.PlayerTimerExpiredDomainEvent;
import dev.alexissdev.kronos.timers.event.PlayerTimerStartedDomainEvent;
import dev.alexissdev.kronos.timers.repository.TimerRepository;
import dev.alexissdev.kronos.timers.service.TimerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class TimerApplicationService implements TimerService<UUID> {

    private static final long COMBAT_TAG_DURATION_MS = 30_000L;

    private final TimerRepository timerRepository;
    private final TimerCache timerCache;
    private final EventBus eventBus;

    @Inject
    public TimerApplicationService(TimerRepository timerRepository,
                                   TimerCache timerCache,
                                   EventBus eventBus) {
        this.timerRepository = timerRepository;
        this.timerCache = timerCache;
        this.eventBus = eventBus;
    }

    @Override
    public CompletableFuture<Void> startTimer(UUID playerUuid, TimerType type, long durationMillis) {
        Instant expiresAt = Instant.now().plusMillis(durationMillis);
        Timer timer = new Timer(playerUuid, type, expiresAt);
        timerCache.markActive(playerUuid, type);
        eventBus.post(new PlayerTimerStartedDomainEvent(playerUuid, type, durationMillis));
        return timerRepository.saveTimer(timer).whenComplete((v, ex) -> {
            if (ex != null) timerCache.markInactive(playerUuid, type);
        });
    }

    @Override
    public CompletableFuture<Void> cancelTimer(UUID playerUuid, TimerType type) {
        timerCache.markInactive(playerUuid, type);
        return timerRepository.deleteTimer(playerUuid, type);
    }

    @Override
    public CompletableFuture<Boolean> hasActiveTimer(UUID playerUuid, TimerType type) {
        return timerRepository.findTimer(playerUuid, type)
                .thenApply(opt -> {
                    boolean active = opt.isPresent() && !opt.get().isExpired();
                    boolean wasActive = timerCache.hasTimer(playerUuid, type);
                    if (active) {
                        timerCache.markActive(playerUuid, type);
                    } else {
                        timerCache.markInactive(playerUuid, type);
                        if (wasActive) {
                            eventBus.post(new PlayerTimerExpiredDomainEvent(playerUuid, type));
                        }
                    }
                    return active;
                });
    }

    @Override
    public CompletableFuture<OptionalLong> getRemainingMillis(UUID playerUuid, TimerType type) {
        return timerRepository.findTimer(playerUuid, type).thenApply(opt ->
                opt.filter(t -> !t.isExpired())
                        .map(t -> OptionalLong.of(t.getRemainingMillis()))
                        .orElse(OptionalLong.empty()));
    }

    public boolean hasActiveTimerSync(UUID playerUuid, TimerType type) {
        return timerCache.hasTimer(playerUuid, type);
    }

    public CompletableFuture<Void> tagForCombat(UUID tagged, UUID tagger) {
        eventBus.post(new PlayerCombatTaggedDomainEvent(tagged, tagger, COMBAT_TAG_DURATION_MS));
        return CompletableFuture.allOf(
                startTimer(tagged, TimerType.COMBAT_TAG, COMBAT_TAG_DURATION_MS),
                startTimer(tagger, TimerType.COMBAT_TAG, COMBAT_TAG_DURATION_MS));
    }

    public CompletableFuture<Void> startPvpTimer(UUID playerUuid, long durationMillis) {
        return startTimer(playerUuid, TimerType.PVP_TIMER, durationMillis);
    }

    public CompletableFuture<Void> startEnderpearlCooldown(UUID playerUuid, long durationMillis) {
        return startTimer(playerUuid, TimerType.ENDERPEARL, durationMillis);
    }

    public CompletableFuture<Void> startLogoutTimer(UUID playerUuid, long durationMillis) {
        return startTimer(playerUuid, TimerType.LOGOUT, durationMillis);
    }

    public CompletableFuture<Void> startHomeTimer(UUID playerUuid, long durationMillis) {
        return startTimer(playerUuid, TimerType.HOME, durationMillis);
    }

    public CompletableFuture<Void> loadTimersIntoCache(UUID playerUuid) {
        List<CompletableFuture<Void>> futures = Arrays.stream(TimerType.values())
                .map(type -> timerRepository.findTimer(playerUuid, type).thenAccept(opt -> {
                    if (opt.isPresent() && !opt.get().isExpired()) {
                        timerCache.markActive(playerUuid, type);
                        // Notify scoreboard of existing timers with remaining time
                        eventBus.post(new PlayerTimerStartedDomainEvent(
                                playerUuid, type, opt.get().getRemainingMillis()));
                    }
                }))
                .collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public void scheduleExpiryChecks(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                for (TimerType type : TimerType.values()) {
                    if (timerCache.hasTimer(uuid, type)) {
                        hasActiveTimer(uuid, type);
                    }
                }
            }
        }, 40L, 40L);
    }
}
