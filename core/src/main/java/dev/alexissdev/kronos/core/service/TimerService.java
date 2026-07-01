package dev.alexissdev.kronos.core.service;

import dev.alexissdev.kronos.core.domain.TimerType;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TimerService<T> {

    CompletableFuture<Void> startTimer(UUID playerUuid, TimerType type, long durationMillis);

    CompletableFuture<Void> cancelTimer(UUID playerUuid, TimerType type);

    CompletableFuture<Boolean> hasActiveTimer(UUID playerUuid, TimerType type);

    CompletableFuture<OptionalLong> getRemainingMillis(UUID playerUuid, TimerType type);
}
