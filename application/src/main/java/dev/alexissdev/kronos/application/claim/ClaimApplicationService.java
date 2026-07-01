package dev.alexissdev.kronos.application.claim;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.domain.Claim;
import dev.alexissdev.kronos.core.domain.ClaimType;
import dev.alexissdev.kronos.core.event.FactionClaimedDomainEvent;
import dev.alexissdev.kronos.core.exception.ClaimConflictException;
import dev.alexissdev.kronos.core.exception.FactionNotFoundException;
import dev.alexissdev.kronos.core.exception.FactionPermissionException;
import dev.alexissdev.kronos.core.repository.ClaimRepository;
import dev.alexissdev.kronos.core.repository.FactionRepository;
import dev.alexissdev.kronos.core.service.ClaimService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ClaimApplicationService implements ClaimService {

    private final ClaimRepository claimRepository;
    private final FactionRepository factionRepository;
    private final EventBus eventBus;

    @Inject
    public ClaimApplicationService(ClaimRepository claimRepository,
                                   FactionRepository factionRepository,
                                   EventBus eventBus) {
        this.claimRepository = claimRepository;
        this.factionRepository = factionRepository;
        this.eventBus = eventBus;
    }

    @Override
    public CompletableFuture<Claim> claim(String factionId, UUID actorUuid, String world,
                                          int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            dev.alexissdev.kronos.core.domain.Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            dev.alexissdev.kronos.core.domain.FactionMember actor = faction.getMember(actorUuid)
                    .orElseThrow(() -> new FactionPermissionException(actorUuid));
            if (!actor.getRole().isAtLeast(dev.alexissdev.kronos.core.domain.FactionRole.CAPTAIN)) {
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
                            eventBus.post(new FactionClaimedDomainEvent(factionId, saved));
                            return saved;
                        });
                    });
        });
    }

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

    @Override
    public CompletableFuture<Void> unclaimAll(String factionId) {
        return claimRepository.deleteByFaction(factionId);
    }

    @Override
    public CompletableFuture<Optional<Claim>> getClaimAt(String world, int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ);
    }

    @Override
    public CompletableFuture<ClaimType> getClaimTypeAt(String world, int chunkX, int chunkZ) {
        return claimRepository.findByChunk(world, chunkX, chunkZ)
                .thenApply(opt -> opt.map(Claim::getType).orElse(ClaimType.WILDERNESS));
    }

    @Override
    public CompletableFuture<List<Claim>> getFactionClaims(String factionId) {
        return claimRepository.findByFaction(factionId);
    }

    @Override
    public CompletableFuture<List<Claim>> getAllClaims() {
        return claimRepository.findAll();
    }

    @Override
    public CompletableFuture<Void> overclaim(String factionId, UUID actorUuid, String world,
                                             int chunkX, int chunkZ) {
        return factionRepository.findById(factionId).thenCompose(attackerOpt -> {
            dev.alexissdev.kronos.core.domain.Faction attacker = attackerOpt.orElseThrow(() -> new FactionNotFoundException(factionId));
            dev.alexissdev.kronos.core.domain.FactionMember actor = attacker.getMember(actorUuid)
                    .orElseThrow(() -> new FactionPermissionException(actorUuid));
            if (!actor.getRole().isAtLeast(dev.alexissdev.kronos.core.domain.FactionRole.CAPTAIN)) {
                throw new FactionPermissionException(actorUuid);
            }
            return claimRepository.findByChunk(world, chunkX, chunkZ).thenCompose(opt -> {
                Claim existing = opt.orElseThrow(() ->
                        new ClaimConflictException("No hay territorio enemigo aquí"));
                if (factionId.equals(existing.getFactionId())) {
                    throw new ClaimConflictException("Este territorio ya es tuyo");
                }
                return claimRepository.delete(existing.getId()).thenCompose(v -> {
                    String id = UUID.randomUUID().toString();
                    Claim newClaim = new Claim(id, factionId, ClaimType.FACTION, world,
                            existing.getMinChunkX(), existing.getMinChunkZ(),
                            existing.getMaxChunkX(), existing.getMaxChunkZ());
                    return claimRepository.save(newClaim).thenAccept(saved ->
                            eventBus.post(new FactionClaimedDomainEvent(factionId, saved)));
                });
            });
        });
    }
}
