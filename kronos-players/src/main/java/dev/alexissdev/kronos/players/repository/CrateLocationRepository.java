package dev.alexissdev.kronos.players.repository;

import dev.alexissdev.kronos.players.domain.CrateLocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CrateLocationRepository {

    CompletableFuture<Optional<CrateLocation>> findByLocation(String world, int x, int y, int z);

    CompletableFuture<List<CrateLocation>> findAll();

    CompletableFuture<CrateLocation> save(CrateLocation location);

    CompletableFuture<Void> delete(String id);
}
