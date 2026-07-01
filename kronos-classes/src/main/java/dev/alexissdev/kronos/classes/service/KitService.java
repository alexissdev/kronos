package dev.alexissdev.kronos.classes.service;

import dev.alexissdev.kronos.players.domain.KitType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface KitService {

    CompletableFuture<Optional<KitType>> detectKit(UUID playerUuid);

    CompletableFuture<Void> activateClassAbility(UUID playerUuid, KitType kitType);

    CompletableFuture<Boolean> isClassAbilityOnCooldown(UUID playerUuid);

    CompletableFuture<Void> updateActiveKit(UUID playerUuid, KitType kitType);
}
