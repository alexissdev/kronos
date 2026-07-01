package dev.alexissdev.kronos.core.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {

    CompletableFuture<Double> getBalance(UUID playerUuid);

    CompletableFuture<Void> deposit(UUID playerUuid, double amount);

    CompletableFuture<Void> withdraw(UUID playerUuid, double amount);

    CompletableFuture<Void> transfer(UUID fromUuid, UUID toUuid, double amount);

    CompletableFuture<Boolean> hasEnoughBalance(UUID playerUuid, double amount);
}
