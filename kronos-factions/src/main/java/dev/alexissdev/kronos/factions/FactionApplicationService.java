package dev.alexissdev.kronos.factions;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import dev.alexissdev.kronos.factions.domain.FactionRole;
import dev.alexissdev.kronos.factions.exception.FactionNotFoundException;
import dev.alexissdev.kronos.factions.exception.FactionPermissionException;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.economy.exception.InsufficientFundsException;
import dev.alexissdev.kronos.factions.event.FactionCreatedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDtkDecrementedDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerJoinedFactionDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerLeftFactionDomainEvent;

import dev.alexissdev.kronos.factions.repository.FactionRepository;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.economy.service.EconomyService;
import dev.alexissdev.kronos.factions.service.FactionService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class FactionApplicationService implements FactionService {

    private final FactionRepository factionRepository;
    private final PlayerRepository playerRepository;
    private final EconomyService economyService;
    private final EventBus eventBus;
    private final int maxMembers;
    private final long reinviteCooldownMs;
    private final Map<UUID, String> pendingInvites          = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Long>   leftFactionTimestamps   = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, String> leftFactionIds          = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject
    public FactionApplicationService(FactionRepository factionRepository,
                                     PlayerRepository playerRepository,
                                     EconomyService economyService,
                                     EventBus eventBus,
                                     @Named("faction.max-members") int maxMembers,
                                     @Named("faction.reinvite-cooldown-ms") long reinviteCooldownMs) {
        this.factionRepository = factionRepository;
        this.playerRepository = playerRepository;
        this.economyService = economyService;
        this.eventBus = eventBus;
        this.maxMembers = maxMembers;
        this.reinviteCooldownMs = reinviteCooldownMs;
    }

    @Override
    public CompletableFuture<Faction> createFaction(String name, UUID leaderId) {
        return factionRepository.findByMember(leaderId).thenCompose(alreadyIn -> {
            if (alreadyIn.isPresent()) {
                throw new HCFException("Ya estás en una facción, sal primero");
            }
            return factionRepository.findByName(name).thenCompose(existing -> {
                if (existing.isPresent()) {
                    throw new HCFException("Ese nombre de facción ya está en uso");
                }
                String id = UUID.randomUUID().toString();
                Faction faction = new Faction(id, name, leaderId, 20, Instant.now());
                faction.addMember(new FactionMember(leaderId, FactionRole.LEADER, Instant.now()));
                return factionRepository.save(faction).thenApply(saved -> {
                    eventBus.post(new FactionCreatedDomainEvent(saved.getId(), saved.getName(), leaderId));
                    return saved;
                });
            });
        });
    }

    @Override
    public CompletableFuture<Void> disbandFaction(String factionId, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, actorUuid, FactionRole.LEADER);
            return factionRepository.delete(factionId).thenRun(() ->
                    eventBus.post(new FactionDisbandedDomainEvent(factionId, faction.getName(), actorUuid)));
        });
    }

    @Override
    public CompletableFuture<Void> renameFaction(String factionId, String newName, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, actorUuid, FactionRole.LEADER);
            faction.setName(newName);
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> inviteMember(String factionId, UUID inviteeUuid, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, actorUuid, FactionRole.CAPTAIN);
            if (faction.isFrozen()) {
                throw new HCFException("Tu facción está congelada, no puedes invitar jugadores");
            }
            if (faction.hasMember(inviteeUuid)) {
                throw new HCFException("Ese jugador ya es miembro de tu facción");
            }
            if (faction.getMembers().size() >= maxMembers) {
                throw new HCFException("La facción está llena (" + maxMembers + " miembros máximo)");
            }
            Long leftAt = leftFactionTimestamps.get(inviteeUuid);
            if (leftAt != null && factionId.equals(leftFactionIds.get(inviteeUuid))) {
                long elapsed = System.currentTimeMillis() - leftAt;
                if (elapsed < reinviteCooldownMs) {
                    long remainingSecs = (reinviteCooldownMs - elapsed) / 1000;
                    throw new HCFException("Ese jugador debe esperar " + remainingSecs + "s antes de ser re-invitado");
                }
            }
            return factionRepository.findByMember(inviteeUuid).thenAccept(existing -> {
                if (existing.isPresent()) {
                    throw new HCFException("Ese jugador ya pertenece a otra facción");
                }
                pendingInvites.put(inviteeUuid, factionId);
            });
        });
    }

    @Override
    public CompletableFuture<Void> acceptInvite(UUID playerUuid, String factionId) {
        if (!factionId.equals(pendingInvites.get(playerUuid))) {
            return CompletableFuture.failedFuture(
                    new HCFException("No tienes invitación pendiente de esa facción"));
        }
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            if (faction.getMembers().size() >= maxMembers) {
                throw new HCFException("La facción está llena (" + maxMembers + " miembros máximo)");
            }
            faction.addMember(new FactionMember(playerUuid, FactionRole.MEMBER, Instant.now()));
            return factionRepository.save(faction).thenRun(() -> {
                pendingInvites.remove(playerUuid);
                eventBus.post(new PlayerJoinedFactionDomainEvent(playerUuid, factionId));
            });
        });
    }

    @Override
    public CompletableFuture<Void> kickMember(String factionId, UUID targetUuid, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            if (actorUuid.equals(targetUuid)) {
                throw new HCFException("No puedes expulsarte a ti mismo");
            }
            requireRole(faction, actorUuid, FactionRole.CAPTAIN);
            FactionMember target = faction.getMember(targetUuid)
                    .orElseThrow(() -> new HCFException("Player is not in this faction"));
            if (target.getRole().isAtLeast(FactionRole.CAPTAIN)) {
                requireRole(faction, actorUuid, FactionRole.CO_LEADER);
            }
            faction.removeMember(targetUuid);
            leftFactionTimestamps.put(targetUuid, System.currentTimeMillis());
            leftFactionIds.put(targetUuid, factionId);
            return factionRepository.save(faction).thenRun(() ->
                    eventBus.post(new PlayerLeftFactionDomainEvent(targetUuid, factionId, true)));
        });
    }

    @Override
    public CompletableFuture<Void> leaveFaction(UUID playerUuid) {
        return factionRepository.findByMember(playerUuid).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new HCFException("You are not in a faction"));
            if (faction.getLeaderId().equals(playerUuid)) {
                throw new HCFException("Leader must disband the faction or transfer leadership first");
            }
            String factionId = faction.getId();
            faction.removeMember(playerUuid);
            leftFactionTimestamps.put(playerUuid, System.currentTimeMillis());
            leftFactionIds.put(playerUuid, factionId);
            return factionRepository.save(faction).thenRun(() ->
                    eventBus.post(new PlayerLeftFactionDomainEvent(playerUuid, factionId, false)));
        });
    }

    @Override
    public CompletableFuture<Void> setRole(String factionId, UUID targetUuid, FactionRole role, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, actorUuid, FactionRole.CO_LEADER);
            FactionMember actor = faction.getMember(actorUuid)
                    .orElseThrow(() -> new HCFException("You are not in this faction"));
            if (role.isAtLeast(actor.getRole())) {
                throw new HCFException("No puedes asignar un rango igual o superior al tuyo");
            }
            FactionMember target = faction.getMember(targetUuid)
                    .orElseThrow(() -> new HCFException("Player is not in this faction"));
            target.setRole(role);
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> setAlly(String factionAId, String factionBId, UUID actorUuid) {
        CompletableFuture<Optional<Faction>> futureA = factionRepository.findById(factionAId);
        CompletableFuture<Optional<Faction>> futureB = factionRepository.findById(factionBId);

        return futureA.thenCombine(futureB, (optA, optB) -> {
            Faction factionA = optA.orElseThrow(() -> new FactionNotFoundException(factionAId));
            Faction factionB = optB.orElseThrow(() -> new FactionNotFoundException(factionBId));
            requireRole(factionA, actorUuid, FactionRole.CO_LEADER);
            if (factionAId.equals(factionBId)) throw new HCFException("No puedes aliarte contigo mismo");
            factionA.removeEnemy(factionBId);
            factionB.removeEnemy(factionAId);
            factionA.addAlly(factionBId);
            factionB.addAlly(factionAId);
            return factionRepository.save(factionA).thenCompose(f -> factionRepository.save(factionB));
        }).thenCompose(f -> f).thenApply(f -> null);
    }

    @Override
    public CompletableFuture<Void> setEnemy(String factionAId, String factionBId, UUID actorUuid) {
        CompletableFuture<Optional<Faction>> futureA = factionRepository.findById(factionAId);
        CompletableFuture<Optional<Faction>> futureB = factionRepository.findById(factionBId);

        return futureA.thenCombine(futureB, (optA, optB) -> {
            Faction factionA = optA.orElseThrow(() -> new FactionNotFoundException(factionAId));
            Faction factionB = optB.orElseThrow(() -> new FactionNotFoundException(factionBId));
            requireRole(factionA, actorUuid, FactionRole.CO_LEADER);
            if (factionAId.equals(factionBId)) throw new HCFException("No puedes ser enemigo de ti mismo");
            factionA.removeAlly(factionBId);
            factionB.removeAlly(factionAId);
            factionA.addEnemy(factionBId);
            factionB.addEnemy(factionAId);
            return factionRepository.save(factionA).thenCompose(f -> factionRepository.save(factionB));
        }).thenCompose(f -> f).thenApply(f -> null);
    }

    @Override
    public CompletableFuture<Void> removeRelation(String factionAId, String factionBId, UUID actorUuid) {
        CompletableFuture<Optional<Faction>> futureA = factionRepository.findById(factionAId);
        CompletableFuture<Optional<Faction>> futureB = factionRepository.findById(factionBId);

        return futureA.thenCombine(futureB, (optA, optB) -> {
            Faction factionA = optA.orElseThrow(() -> new FactionNotFoundException(factionAId));
            Faction factionB = optB.orElseThrow(() -> new FactionNotFoundException(factionBId));
            requireRole(factionA, actorUuid, FactionRole.CO_LEADER);
            factionA.removeAlly(factionBId);
            factionA.removeEnemy(factionBId);
            factionB.removeAlly(factionAId);
            factionB.removeEnemy(factionAId);
            return factionRepository.save(factionA).thenCompose(f -> factionRepository.save(factionB));
        }).thenCompose(f -> f).thenApply(f -> null);
    }

    @Override
    public CompletableFuture<Void> deposit(String factionId, UUID playerUuid, double amount) {
        if (amount <= 0) return CompletableFuture.failedFuture(
                new HCFException("La cantidad debe ser mayor a 0"));
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            if (faction.isFrozen()) {
                throw new HCFException("Tu facción está congelada, no puedes depositar");
            }
            return economyService.withdraw(playerUuid, amount).thenCompose(v -> {
                faction.deposit(amount);
                return factionRepository.save(faction).thenApply(f -> null);
            });
        });
    }

    @Override
    public CompletableFuture<Void> withdraw(String factionId, UUID playerUuid, double amount) {
        if (amount <= 0) return CompletableFuture.failedFuture(
                new HCFException("La cantidad debe ser mayor a 0"));
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, playerUuid, FactionRole.CO_LEADER);
            if (faction.getBalance() < amount) {
                throw new InsufficientFundsException(amount, faction.getBalance());
            }
            faction.withdraw(amount);
            return factionRepository.save(faction)
                    .thenCompose(f -> economyService.deposit(playerUuid, amount));
        });
    }

    @Override
    public CompletableFuture<Optional<Faction>> getByPlayer(UUID playerUuid) {
        return factionRepository.findByMember(playerUuid);
    }

    @Override
    public CompletableFuture<Optional<Faction>> getById(String id) {
        return factionRepository.findById(id);
    }

    @Override
    public CompletableFuture<Optional<Faction>> getByName(String name) {
        return factionRepository.findByName(name);
    }

    @Override
    public CompletableFuture<List<Faction>> getTopFactions(int limit) {
        return factionRepository.findTopByKills(limit);
    }

    @Override
    public CompletableFuture<Void> notifyMemberDeath(String factionId, UUID deadMemberUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            faction.incrementDeaths();
            if (faction.hasMember(deadMemberUuid)) {
                faction.decrementDtk();
                if (faction.isAtDtk()) {
                    return factionRepository.delete(factionId).thenRun(() ->
                            eventBus.post(new FactionDisbandedDomainEvent(
                                    factionId, faction.getName(), deadMemberUuid)));
                }
                eventBus.post(new FactionDtkDecrementedDomainEvent(
                        factionId, faction.getName(), faction.getDtkRemaining(), faction.getMaxDtk()));
            }
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> setFactionHome(String factionId, UUID actorUuid, dev.alexissdev.kronos.factions.domain.FactionHome home) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, actorUuid, FactionRole.CAPTAIN);
            faction.setHome(home);
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> clearFactionHome(String factionId, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, actorUuid, FactionRole.CAPTAIN);
            faction.clearHome();
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> addStrike(String factionId, String reason, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            faction.addStrike();
            if (faction.isAtMaxStrikes()) {
                return factionRepository.delete(factionId).thenRun(() ->
                        eventBus.post(new FactionDisbandedDomainEvent(factionId, faction.getName(), actorUuid)));
            }
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> freezeFaction(String factionId, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            faction.setFrozen(true);
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> unfreezeFaction(String factionId, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            faction.setFrozen(false);
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    @Override
    public CompletableFuture<Void> setLeader(String factionId, UUID newLeaderUuid, UUID actorUuid) {
        return factionRepository.findById(factionId).thenCompose(opt -> {
            Faction faction = opt.orElseThrow(() -> new FactionNotFoundException(factionId));
            requireRole(faction, actorUuid, FactionRole.LEADER);
            if (actorUuid.equals(newLeaderUuid)) {
                throw new HCFException("Ya eres el líder de esta facción");
            }
            FactionMember newLeader = faction.getMember(newLeaderUuid)
                    .orElseThrow(() -> new HCFException("Ese jugador no está en la facción"));
            FactionMember oldLeader = faction.getMember(actorUuid)
                    .orElseThrow(() -> new HCFException("You are not in this faction"));
            oldLeader.setRole(FactionRole.CO_LEADER);
            newLeader.setRole(FactionRole.LEADER);
            faction.setLeaderId(newLeaderUuid);
            return factionRepository.save(faction).thenApply(f -> null);
        });
    }

    private void requireRole(Faction faction, UUID actorUuid, FactionRole required) {
        FactionMember actor = faction.getMember(actorUuid)
                .orElseThrow(() -> new HCFException("You are not in this faction"));
        if (!actor.getRole().isAtLeast(required)) {
            throw new FactionPermissionException(required);
        }
    }
}
