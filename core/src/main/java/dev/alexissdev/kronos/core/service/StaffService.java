package dev.alexissdev.kronos.core.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StaffService {

    CompletableFuture<Void> enableStaffMode(UUID staffUuid);

    CompletableFuture<Void> disableStaffMode(UUID staffUuid);

    CompletableFuture<Void> setVanish(UUID staffUuid, boolean vanished);

    CompletableFuture<Void> freeze(UUID staffUuid, UUID targetUuid);

    CompletableFuture<Void> unfreeze(UUID targetUuid);

    CompletableFuture<Boolean> isFrozen(UUID playerUuid);

    CompletableFuture<Boolean> isInStaffMode(UUID staffUuid);

    CompletableFuture<Boolean> isVanished(UUID playerUuid);
}
