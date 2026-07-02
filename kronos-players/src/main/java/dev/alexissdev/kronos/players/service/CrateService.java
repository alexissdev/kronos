package dev.alexissdev.kronos.players.service;

import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.domain.CrateLocation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CrateService {

    CompletableFuture<CrateLocation> setCrate(String world, int x, int y, int z, CrateType type);

    CompletableFuture<Void> removeCrate(String world, int x, int y, int z);

    CompletableFuture<Optional<CrateLocation>> getCrateAt(String world, int x, int y, int z);

    CompletableFuture<List<CrateLocation>> getAllCrates();
}
