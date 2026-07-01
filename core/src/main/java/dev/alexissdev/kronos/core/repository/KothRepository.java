package dev.alexissdev.kronos.core.repository;

import dev.alexissdev.kronos.core.domain.KothZone;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Async CRUD operations for KothZone entities. */
public interface KothRepository {

    CompletableFuture<Optional<KothZone>> findByName(String name);

    CompletableFuture<List<KothZone>> findAll();

    CompletableFuture<KothZone> save(KothZone zone);

    CompletableFuture<Void> delete(String name);
}
