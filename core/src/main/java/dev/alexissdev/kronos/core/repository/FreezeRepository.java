package dev.alexissdev.kronos.core.repository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistent store for frozen player state backed by Redis. */
public interface FreezeRepository {

    CompletableFuture<Void> freeze(UUID staffUuid, UUID targetUuid);

    CompletableFuture<Void> unfreeze(UUID targetUuid);

    CompletableFuture<Boolean> isFrozen(UUID targetUuid);
}
