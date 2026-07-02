package dev.alexissdev.kronos.players.repository;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DeathbanRepository {

    CompletableFuture<Void> setDeathban(UUID uuid, long durationSeconds);

    CompletableFuture<OptionalLong> getRemainingSeconds(UUID uuid);

    CompletableFuture<Void> removeDeathban(UUID uuid);
}
