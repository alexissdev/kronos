package dev.alexissdev.kronos.core.service;

import dev.alexissdev.kronos.core.domain.HCFPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerService {

    CompletableFuture<HCFPlayer> getOrCreate(UUID uuid, String name);

    CompletableFuture<Optional<HCFPlayer>> getPlayer(UUID uuid);

    CompletableFuture<Void> savePlayer(HCFPlayer player);

    CompletableFuture<Void> recordKill(UUID killerUuid, UUID victimUuid);
}
