package dev.alexissdev.kronos.factions.repository;

import dev.alexissdev.kronos.factions.domain.Faction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Async CRUD operations for Faction aggregates. */
public interface FactionRepository {

    CompletableFuture<Optional<Faction>> findById(String id);

    CompletableFuture<Optional<Faction>> findByName(String name);

    CompletableFuture<Optional<Faction>> findByMember(UUID playerUuid);

    CompletableFuture<List<Faction>> findTopByKills(int limit);

    CompletableFuture<List<Faction>> findRaidable();

    CompletableFuture<Faction> save(Faction faction);

    CompletableFuture<Void> delete(String id);
}
