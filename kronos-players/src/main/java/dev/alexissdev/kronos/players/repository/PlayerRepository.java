package dev.alexissdev.kronos.players.repository;

import dev.alexissdev.kronos.players.domain.HCFPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Async CRUD operations for HCFPlayer entities. */
public interface PlayerRepository {

    CompletableFuture<Optional<HCFPlayer>> findByUuid(UUID uuid);

    CompletableFuture<HCFPlayer> save(HCFPlayer player);
}
