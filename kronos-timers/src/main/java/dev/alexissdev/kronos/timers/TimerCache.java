package dev.alexissdev.kronos.timers;

import com.google.inject.Singleton;
import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory mirror of active timers for synchronous lookups in event handlers.
 * Redis is the source of truth; this cache prevents async calls inside Bukkit events.
 */
@Singleton
public class TimerCache {

    private final Map<UUID, Set<TimerType>>    activeTimers = new ConcurrentHashMap<>();
    private final Map<UUID, Map<TimerType, Long>> expiryMs = new ConcurrentHashMap<>();

    public void markActive(UUID playerUuid, TimerType type, long expiryEpochMs) {
        activeTimers.computeIfAbsent(playerUuid, k ->
                Collections.synchronizedSet(EnumSet.noneOf(TimerType.class))).add(type);
        expiryMs.computeIfAbsent(playerUuid, k ->
                Collections.synchronizedMap(new EnumMap<>(TimerType.class))).put(type, expiryEpochMs);
    }

    public void markActive(UUID playerUuid, TimerType type) {
        markActive(playerUuid, type, Long.MAX_VALUE);
    }

    public void markInactive(UUID playerUuid, TimerType type) {
        Set<TimerType> timers = activeTimers.get(playerUuid);
        if (timers != null) timers.remove(type);
        Map<TimerType, Long> expiries = expiryMs.get(playerUuid);
        if (expiries != null) expiries.remove(type);
    }

    public boolean hasTimer(UUID playerUuid, TimerType type) {
        Set<TimerType> timers = activeTimers.get(playerUuid);
        return timers != null && timers.contains(type);
    }

    public long getRemainingMillis(UUID playerUuid, TimerType type) {
        Map<TimerType, Long> expiries = expiryMs.get(playerUuid);
        if (expiries == null) return 0L;
        Long expiry = expiries.get(type);
        if (expiry == null) return 0L;
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    public void clearAll(UUID playerUuid) {
        activeTimers.remove(playerUuid);
        expiryMs.remove(playerUuid);
    }
}
