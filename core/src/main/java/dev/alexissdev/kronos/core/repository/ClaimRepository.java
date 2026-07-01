package dev.alexissdev.kronos.core.repository;

import dev.alexissdev.kronos.core.domain.Claim;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ClaimRepository {

    CompletableFuture<Optional<Claim>> findByChunk(String world, int chunkX, int chunkZ);

    CompletableFuture<List<Claim>> findByFaction(String factionId);

    CompletableFuture<List<Claim>> findAll();

    CompletableFuture<Claim> save(Claim claim);

    CompletableFuture<Void> deleteByFaction(String factionId);

    CompletableFuture<Void> delete(String id);
}
