package dev.alexissdev.kronos.timers.repository;

import dev.alexissdev.kronos.timers.domain.Timer;
import dev.alexissdev.kronos.timers.domain.TimerType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Async timer persistence backed by Redis with TTL. */
public interface TimerRepository {

    CompletableFuture<Optional<Timer>> findTimer(UUID playerUuid, TimerType type);

    CompletableFuture<List<Timer>> findAllTimers(UUID playerUuid);

    CompletableFuture<Void> saveTimer(Timer timer);

    CompletableFuture<Void> deleteTimer(UUID playerUuid, TimerType type);

    CompletableFuture<Boolean> hasTimer(UUID playerUuid, TimerType type);
}
