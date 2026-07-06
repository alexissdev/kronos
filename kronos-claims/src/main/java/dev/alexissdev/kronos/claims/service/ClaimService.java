package dev.alexissdev.kronos.claims.service;

import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Application port exposing the high-level operations over territories (claims).
 *
 * <p>Defines the contract that claim services must implement in the HCF system.
 * The primary implementation is
 * {@link dev.alexissdev.kronos.claims.ClaimApplicationService}, injected by Guice.
 * All operations are asynchronous and return a {@link CompletableFuture} to avoid
 * blocking the main Bukkit server thread.</p>
 */
public interface ClaimService {

    /**
     * Claims a rectangular area of chunks on behalf of a faction.
     *
     * <p>Validates that the actor holds a {@code CAPTAIN} rank or higher within the
     * faction and that no chunk in the area is already claimed. If validation succeeds,
     * the new claim is persisted and a
     * {@link dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent} is published.</p>
     *
     * @param factionId identificador de la facción que reclama el territorio
     * @param actorUuid UUID of the player performing the action
     * @param world     name of the world where the territory is being claimed
     * @param minChunkX minimum X coordinate of the area (inclusive)
     * @param minChunkZ minimum Z coordinate of the area (inclusive)
     * @param maxChunkX maximum X coordinate of the area (inclusive)
     * @param maxChunkZ maximum Z coordinate of the area (inclusive)
     * @return future with the created and persisted {@link Claim}
     * @throws dev.alexissdev.kronos.claims.exception.ClaimConflictException if any chunk in the area is already occupied
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException if the actor does not hold the required rank
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException    if the faction does not exist
     */
    CompletableFuture<Claim> claim(String factionId, UUID actorUuid, String world,
                                   int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ);

    /**
     * Releases the territory of the specified chunk by removing the claim that occupies it.
     *
     * <p>Only the claim's owner may unclaim it. The actor must be a member of the faction
     * that owns the chunk.</p>
     *
     * @param factionId identifier of the faction attempting to unclaim
     * @param actorUuid UUID of the player performing the action
     * @param world     name of the world where the chunk is located
     * @param chunkX    X coordinate of the chunk to release
     * @param chunkZ    Z coordinate of the chunk to release
     * @return future that completes when the claim has been deleted
     * @throws dev.alexissdev.kronos.claims.exception.ClaimConflictException       if the chunk has no claim
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException if the faction does not own the chunk
     */
    CompletableFuture<Void> unclaim(String factionId, UUID actorUuid, String world, int chunkX, int chunkZ);

    /**
     * Deletes all claims belonging to a faction in a single bulk operation.
     *
     * <p>Used primarily when disbanding a faction to release all of its territory at once.</p>
     *
     * @param factionId identifier of the faction whose territory is being released
     * @return future that completes when all claims have been deleted
     */
    CompletableFuture<Void> unclaimAll(String factionId);

    /**
     * Returns the claim occupying the specified chunk, if one exists.
     *
     * @param world  name of the world to search in
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return future with the claim found, or empty if the chunk is unclaimed wilderness
     */
    CompletableFuture<Optional<Claim>> getClaimAt(String world, int chunkX, int chunkZ);

    /**
     * Returns the territory type at the given chunk coordinates.
     *
     * <p>If the chunk has no claim, {@link ClaimType#WILDERNESS} is returned as the default.</p>
     *
     * @param world  name of the world to query
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return future with the {@link ClaimType} of the territory
     */
    CompletableFuture<ClaimType> getClaimTypeAt(String world, int chunkX, int chunkZ);

    /**
     * Retrieves the complete list of claims owned by a faction.
     *
     * @param factionId faction identifier
     * @return future with all claims of the faction; empty list if it has no territory
     */
    CompletableFuture<List<Claim>> getFactionClaims(String factionId);

    /**
     * Retrieves every claim currently registered on the server.
     *
     * <p>Primarily used to preload the in-memory cache when the plugin starts up.</p>
     *
     * @return future with the list of all claims
     */
    CompletableFuture<List<Claim>> getAllClaims();

    /**
     * Allows a faction to conquer the territory of an enemy or raidable faction.
     *
     * <p>The actor must hold a {@code CAPTAIN} rank or higher. The target chunk must
     * belong to a faction that is either an enemy of the attacker or currently in a
     * raidable state. The existing claim is deleted and a new one is created under the
     * attacking faction's ownership.</p>
     *
     * @param factionId identifier of the faction conquering the territory
     * @param actorUuid UUID of the player performing the action
     * @param world     name of the world where the chunk is located
     * @param chunkX    X coordinate of the chunk to conquer
     * @param chunkZ    Z coordinate of the chunk to conquer
     * @return future that completes when the conquest has been recorded
     * @throws dev.alexissdev.kronos.claims.exception.ClaimConflictException       if the chunk is already owned or conditions are not met
     * @throws dev.alexissdev.kronos.factions.exception.FactionPermissionException if the actor does not hold the required rank
     * @throws dev.alexissdev.kronos.factions.exception.FactionNotFoundException   if the attacking faction does not exist
     */
    CompletableFuture<Void> overclaim(String factionId, UUID actorUuid, String world, int chunkX, int chunkZ);
}
