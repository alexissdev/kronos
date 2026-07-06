package dev.alexissdev.kronos.claims.repository;

import dev.alexissdev.kronos.claims.domain.Claim;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence port for the {@link dev.alexissdev.kronos.claims.domain.Claim} entity.
 *
 * <p>Defines the contract that all storage implementations of territories (claims) must
 * fulfil. The current concrete implementation is
 * {@link dev.alexissdev.kronos.claims.persistence.MongoClaimRepository}, backed by
 * MongoDB. All operations are asynchronous and return a {@link CompletableFuture} to
 * avoid blocking the main Bukkit server thread.</p>
 */
public interface ClaimRepository {

    /**
     * Finds the claim that contains the specified chunk within a given world.
     *
     * <p>The lookup checks whether the chunk coordinates fall inside the rectangle
     * {@code [minChunkX, maxChunkX] × [minChunkZ, maxChunkZ]} stored in the claim.</p>
     *
     * @param world  name of the Minecraft world to search in
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return future with the matching claim, or {@link java.util.Optional#empty()} if the
     *         chunk is unclaimed wilderness
     */
    CompletableFuture<Optional<Claim>> findByChunk(String world, int chunkX, int chunkZ);

    /**
     * Retrieves all claims belonging to a specific faction.
     *
     * @param factionId identifier of the owning faction
     * @return future with the list of the faction's claims; empty list if it has no territory
     */
    CompletableFuture<List<Claim>> findByFaction(String factionId);

    /**
     * Retrieves every claim stored in the database.
     *
     * <p>Used primarily at server startup to preload the in-memory cache in
     * {@link dev.alexissdev.kronos.claims.listener.ClaimListener}.</p>
     *
     * @return future with the complete list of all claims
     */
    CompletableFuture<List<Claim>> findAll();

    /**
     * Persists a new claim or updates an existing one (upsert by {@code id}).
     *
     * @param claim domain entity to save
     * @return future with the same claim after the write is confirmed
     */
    CompletableFuture<Claim> save(Claim claim);

    /**
     * Deletes all claims belonging to a faction.
     *
     * <p>Called when a faction is disbanded to release all of its territory.</p>
     *
     * @param factionId identifier of the faction whose territory is being freed
     * @return future that completes when all claims have been deleted
     */
    CompletableFuture<Void> deleteByFaction(String factionId);

    /**
     * Deletes the claim with the specified identifier.
     *
     * @param id UUID as a String of the claim to delete
     * @return future that completes when the claim has been deleted
     */
    CompletableFuture<Void> delete(String id);
}
