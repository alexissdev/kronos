package dev.alexissdev.kronos.spawn.repository;

import dev.alexissdev.kronos.spawn.domain.SpawnZone;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface SpawnRepository {

    CompletableFuture<Optional<SpawnZone>> findZone();

    CompletableFuture<Void> saveZone(SpawnZone zone);

    CompletableFuture<Void> deleteZone();
}
