package dev.alexissdev.kronos.claims.service;

import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ClaimService {

    CompletableFuture<Claim> claim(String factionId, UUID actorUuid, String world,
                                   int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ);

    CompletableFuture<Void> unclaim(String factionId, UUID actorUuid, String world, int chunkX, int chunkZ);

    CompletableFuture<Void> unclaimAll(String factionId);

    CompletableFuture<Optional<Claim>> getClaimAt(String world, int chunkX, int chunkZ);

    CompletableFuture<ClaimType> getClaimTypeAt(String world, int chunkX, int chunkZ);

    CompletableFuture<List<Claim>> getFactionClaims(String factionId);

    CompletableFuture<List<Claim>> getAllClaims();

    CompletableFuture<Void> overclaim(String factionId, UUID actorUuid, String world, int chunkX, int chunkZ);
}
