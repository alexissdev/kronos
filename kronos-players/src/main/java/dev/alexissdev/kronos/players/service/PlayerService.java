package dev.alexissdev.kronos.players.service;

import dev.alexissdev.kronos.players.domain.HCFPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerService {

    CompletableFuture<HCFPlayer> getOrCreate(UUID uuid, String name);

    CompletableFuture<Optional<HCFPlayer>> getPlayer(UUID uuid);

    CompletableFuture<Void> savePlayer(HCFPlayer player);

    CompletableFuture<Void> recordKill(UUID killerUuid, UUID victimUuid);

    CompletableFuture<Integer> decrementLives(UUID uuid);
}
