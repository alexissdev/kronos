package dev.alexissdev.kronos.factions.service;

import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionHome;
import dev.alexissdev.kronos.factions.domain.FactionRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FactionService {

    CompletableFuture<Faction> createFaction(String name, UUID leaderId);

    CompletableFuture<Void> disbandFaction(String factionId, UUID actorUuid);

    CompletableFuture<Void> renameFaction(String factionId, String newName, UUID actorUuid);

    CompletableFuture<Void> inviteMember(String factionId, UUID inviteeUuid, UUID actorUuid);

    CompletableFuture<Void> acceptInvite(UUID playerUuid, String factionId);

    CompletableFuture<Void> kickMember(String factionId, UUID targetUuid, UUID actorUuid);

    CompletableFuture<Void> leaveFaction(UUID playerUuid);

    CompletableFuture<Void> setRole(String factionId, UUID targetUuid, FactionRole role, UUID actorUuid);

    CompletableFuture<Void> setAlly(String factionAId, String factionBId, UUID actorUuid);

    CompletableFuture<Void> setEnemy(String factionAId, String factionBId, UUID actorUuid);

    CompletableFuture<Void> removeRelation(String factionAId, String factionBId, UUID actorUuid);

    CompletableFuture<Void> deposit(String factionId, UUID playerUuid, double amount);

    CompletableFuture<Void> withdraw(String factionId, UUID playerUuid, double amount);

    CompletableFuture<Optional<Faction>> getByPlayer(UUID playerUuid);

    CompletableFuture<Optional<Faction>> getById(String id);

    CompletableFuture<Optional<Faction>> getByName(String name);

    CompletableFuture<List<Faction>> getTopFactions(int limit);

    CompletableFuture<Void> notifyMemberDeath(String factionId, UUID deadMemberUuid);

    CompletableFuture<Void> setFactionHome(String factionId, UUID actorUuid, FactionHome home);

    CompletableFuture<Void> clearFactionHome(String factionId, UUID actorUuid);

    CompletableFuture<Void> addStrike(String factionId, String reason, UUID actorUuid);

    CompletableFuture<Void> freezeFaction(String factionId, UUID actorUuid);

    CompletableFuture<Void> unfreezeFaction(String factionId, UUID actorUuid);
}
