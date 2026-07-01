package dev.alexissdev.kronos.spawn.service;

import dev.alexissdev.kronos.spawn.domain.SpawnZone;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SpawnService {

    CompletableFuture<Void> setZone(SpawnZone zone);

    CompletableFuture<Void> removeZone();

    Optional<SpawnZone> getZone();

    CompletableFuture<Void> loadZone();
}
