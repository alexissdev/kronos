package dev.alexissdev.kronos.core.service;

import dev.alexissdev.kronos.core.domain.KothZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface KotHService {

    CompletableFuture<Void> startKoth(String name);

    CompletableFuture<Void> endKoth(String name);

    CompletableFuture<Optional<KothZone>> getKoth(String name);

    CompletableFuture<List<KothZone>> getActiveKoths();

    CompletableFuture<List<KothZone>> getAllKoths();

    CompletableFuture<Void> captureKoth(String name, UUID captorUuid);

    CompletableFuture<Void> createKoth(KothZone zone);

    CompletableFuture<Void> deleteKoth(String name);
}
