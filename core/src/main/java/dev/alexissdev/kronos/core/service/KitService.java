package dev.alexissdev.kronos.core.service;

import dev.alexissdev.kronos.core.domain.KitType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface KitService {

    CompletableFuture<Optional<KitType>> detectKit(UUID playerUuid);

    CompletableFuture<Void> activateClassAbility(UUID playerUuid, KitType kitType);

    CompletableFuture<Boolean> isClassAbilityOnCooldown(UUID playerUuid);

    CompletableFuture<Void> updateActiveKit(UUID playerUuid, KitType kitType);
}
