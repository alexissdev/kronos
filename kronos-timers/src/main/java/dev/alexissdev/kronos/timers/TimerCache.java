package dev.alexissdev.kronos.timers;

import com.google.inject.Singleton;
import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory mirror of active timers for synchronous lookups in event handlers.
 * Redis is the source of truth; this cache prevents async calls inside Bukkit events.
 */
@Singleton
public class TimerCache {

    private final ConcurrentHashMap<UUID, Set<TimerType>> activeTimers = new ConcurrentHashMap<>();

    public void markActive(UUID playerUuid, TimerType type) {
        activeTimers.computeIfAbsent(playerUuid, k ->
                Collections.synchronizedSet(EnumSet.noneOf(TimerType.class))).add(type);
    }

    public void markInactive(UUID playerUuid, TimerType type) {
        Set<TimerType> timers = activeTimers.get(playerUuid);
        if (timers != null) {
            timers.remove(type);
        }
    }

    public boolean hasTimer(UUID playerUuid, TimerType type) {
        Set<TimerType> timers = activeTimers.get(playerUuid);
        return timers != null && timers.contains(type);
    }

    public void clearAll(UUID playerUuid) {
        activeTimers.remove(playerUuid);
    }
}
