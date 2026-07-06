package dev.alexissdev.kronos.claims;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent;
import dev.alexissdev.kronos.claims.exception.ClaimConflictException;
import dev.alexissdev.kronos.factions.exception.FactionNotFoundException;
import dev.alexissdev.kronos.factions.exception.FactionPermissionException;
import dev.alexissdev.kronos.claims.repository.ClaimRepository;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import dev.alexissdev.kronos.factions.domain.FactionRole;
import dev.alexissdev.kronos.factions.repository.FactionRepository;
import dev.alexissdev.kronos.claims.service.ClaimService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Primary application service implementation for HCF territory (claim) management.
 *
 * <p>Orchestrates all business rules related to claiming, releasing, and conquering
 * territories in the Minecraft world. Serves as the entry point between player commands
 * and the domain model, coordinating the claim and faction repositories and publishing
 * domain events on the Guava {@link EventBus} so that other modules (such as the claim
 * listener) can react in a decoupled manner.</p>
 *
 * <p>Registered as a singleton by Guice through {@link ClaimsModule}.</p>
 */
@Singleton
public class ClaimApplicationService implements ClaimService {

    private final ClaimRepository claimRepository;
    private final FactionRepository factionRepository;
    private final EventBus eventBus;

    /**
     * Constructs the service by injecting its dependencies.
     *
     * @param claimRepository   claim persistence repository (MongoDB)
     * @param factionRepository faction repository used to validate actor permissions
     * @param eventBus          Guava event bus for publishing domain events
     */
    @Inject
    public ClaimApplicationService(ClaimRepository claimRepository,
                                   FactionRepository factionRepository,
                                   EventBus eventBus) {
        this.claimRepository = claimRepository;
        this.factionRepository = factionRepository;
        this.eventBus = eventBus;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The process is:
     * <ol>
     *   <li>Verifies that the faction exists and that the actor is a {@code CAPTAIN} or higher.</li>
     *   <li>Checks in parallel that no chunk in the area is already claimed.</li>
     *   <li>Creates and persists the {@link Claim} with type {@link ClaimType#FACTION}.</li>
     *   <li>Publishes a {@link dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent}.</li>
     * </ol></p>
     */
    @Override
    public CompletableFuture<Claim> claim(String factionId, UUID actorUuid, String world,
                                          int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            FactionMember actor = faction.getMember(actorUuid)
                    .orElseThrow(() -> new FactionPermissionException(actorUuid));
            if (!actor.getRole().isAtLeast(FactionRole.CAPTAIN)) {
                throw new FactionPermissionException(actorUuid);
            }
            List<CompletableFuture<Optional<Claim>>> checks = new ArrayList<>();
            for (int x = minChunkX; x <= maxChunkX; x++) {
                for (int z = minChunkZ; z <= maxChunkZ; z++) {
                    checks.add(claimRepository.findByChunk(world, x, z));
                }
            }
            return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
                    .thenCompose(v -> {
                        boolean conflict = checks.stream().anyMatch(f -> f.join().isPresent());
                        if (conflict) throw new ClaimConflictException("Ese territorio ya está reclamado");
                        String id = UUID.randomUUID().toString();
                        Claim claim = new Claim(id, factionId, ClaimType.FACTION, world,
                                minChunkX, minChunkZ, maxChunkX, maxChunkZ);
                        return claimRepository.save(claim).thenApply(saved -> {
                            eventBus.post(new FactionClaimedDomainEvent(factionId, saved.getId(), saved.getType().name(), saved.getWorld(), saved.getMinChunkX(), saved.getMinChunkZ(), saved.getMaxChunkX(), saved.getMaxChunkZ()));
                            return saved;
                        });
                    });
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Finds the claim at the given chunk and deletes it only if the requesting faction
     * is its owner. Throws {@link dev.alexissdev.kronos.factions.exception.FactionPermissionException}
     * if the chunk belongs to a different faction.</p>
     */
    @Override
    public CompletableFuture<Void> unclaim(String factionId, UUID actorUuid, String world,
                                           int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ).thenCompose(opt -> {
            Claim claim = opt.orElseThrow(() -> new ClaimConflictException("No hay ningún claim aquí"));
            if (!factionId.equals(claim.getFactionId())) {
                throw new FactionPermissionException(actorUuid);
            }
            return claimRepository.delete(claim.getId());
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates directly to {@link ClaimRepository#deleteByFaction(String)} to remove
     * all of the faction's claims in a single database operation.</p>
     */
    @Override
    public CompletableFuture<Void> unclaimAll(String factionId) {
        return claimRepository.deleteByFaction(factionId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries the repository directly without additional logic; useful for retrieving
     * all claim metadata (owner, type, bounds) at a specific location.</p>
     */
    @Override
    public CompletableFuture<Optional<Claim>> getClaimAt(String world, int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the chunk has no registered claim, returns {@link ClaimType#WILDERNESS}
     * as the default to indicate unclaimed open land.</p>
     */
    @Override
    public CompletableFuture<ClaimType> getClaimTypeAt(String world, int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ)
                .thenApply(opt -> opt.map(Claim::getType).orElse(ClaimType.WILDERNESS));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns all territories currently registered to the faction, useful for
     * displaying statistics or calculating the power required to hold them.</p>
     */
    @Override
    public CompletableFuture<List<Claim>> getFactionClaims(String factionId) {
        return claimRepository.findByFaction(factionId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Used primarily at server startup to preload the in-memory cache of
     * {@link dev.alexissdev.kronos.claims.listener.ClaimListener}.</p>
     */
    @Override
    public CompletableFuture<List<Claim>> getAllClaims() {
        return claimRepository.findAll();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The overclaim flow is:
     * <ol>
     *   <li>Verifies that the actor is a {@code CAPTAIN} or higher in their faction.</li>
     *   <li>Checks that an enemy claim exists at the target chunk.</li>
     *   <li>Validates that the defending faction is an enemy of the attacker or is raidable.</li>
     *   <li>Delegates to {@link #doOverclaim(String, Claim)} to replace the claim.</li>
     * </ol></p>
     */
    @Override
    public CompletableFuture<Void> overclaim(String factionId, UUID actorUuid, String world,
                                             int chunkX, int chunkZ) {
        return factionRepository.findById(factionId).thenCompose(attackerOpt -> {
            Faction attacker = attackerOpt.orElseThrow(() -> new FactionNotFoundException(factionId));
            FactionMember actor = attacker.getMember(actorUuid)
                    .orElseThrow(() -> new FactionPermissionException(actorUuid));
            if (!actor.getRole().isAtLeast(FactionRole.CAPTAIN)) {
                throw new FactionPermissionException(actorUuid);
            }
            return claimRepository.findByChunk(world, chunkX, chunkZ).thenCompose(opt -> {
                Claim existing = opt.orElseThrow(() ->
                        new ClaimConflictException("No hay territorio enemigo aquí"));
                if (factionId.equals(existing.getFactionId())) {
                    throw new ClaimConflictException("Este territorio ya es tuyo");
                }
                // Only allow overclaiming faction claims that are raidable OR from an enemy faction
                if (existing.getType() == ClaimType.FACTION && existing.getFactionId() != null) {
                    boolean enemy = attacker.isEnemy(existing.getFactionId());
                    if (!enemy) {
                        return factionRepository.findById(existing.getFactionId()).thenCompose(defenderOpt -> {
                            boolean raidable = defenderOpt.map(Faction::isRaidable).orElse(false);
                            if (!raidable) {
                                throw new ClaimConflictException("Esa facción no es tu enemiga ni está siendo raideada");
                            }
                            return doOverclaim(factionId, existing);
                        });
                    }
                }
                return doOverclaim(factionId, existing);
            });
        });
    }

    /**
     * Performs the actual replacement of an existing claim with a new one owned by the attacking faction.
     *
     * <p>Deletes the defender's claim, creates a new {@link Claim} of type
     * {@link ClaimType#FACTION} with the same geographic bounds, and associates it with
     * {@code factionId}. Publishes a
     * {@link dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent} so that the
     * claim listener's cache is updated accordingly.</p>
     *
     * @param factionId identifier of the faction conquering the territory
     * @param existing  existing claim that will be replaced
     * @return future that completes when the new claim has been persisted
     */
    private CompletableFuture<Void> doOverclaim(String factionId, Claim existing) {
        return claimRepository.delete(existing.getId()).thenCompose(v -> {
            String id = UUID.randomUUID().toString();
            Claim newClaim = new Claim(id, factionId, ClaimType.FACTION, existing.getWorld(),
                    existing.getMinChunkX(), existing.getMinChunkZ(),
                    existing.getMaxChunkX(), existing.getMaxChunkZ());
            return claimRepository.save(newClaim).thenAccept(saved ->
                    eventBus.post(new FactionClaimedDomainEvent(factionId, saved.getId(), saved.getType().name(),
                            saved.getWorld(), saved.getMinChunkX(), saved.getMinChunkZ(),
                            saved.getMaxChunkX(), saved.getMaxChunkZ())));
        });
    }
}
