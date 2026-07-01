package dev.alexissdev.kronos.infrastructure.vault;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.exception.HCFException;
import dev.alexissdev.kronos.core.exception.InsufficientFundsException;
import dev.alexissdev.kronos.core.service.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Singleton
public class VaultEconomyService implements EconomyService {

    private final Economy economy;
    private final Executor mainThreadExecutor;

    @Inject
    public VaultEconomyService(Economy economy, Plugin plugin) {
        this.economy = economy;
        this.mainThreadExecutor = runnable -> {
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        };
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            return economy.getBalance(player);
        }, mainThreadExecutor);
    }

    @Override
    public CompletableFuture<Void> deposit(UUID playerUuid, double amount) {
        if (amount <= 0) return CompletableFuture.failedFuture(
                new HCFException("La cantidad debe ser mayor a 0"));
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            EconomyResponse response = economy.depositPlayer(player, amount);
            if (!response.transactionSuccess())
                throw new HCFException("Error al depositar: " + response.errorMessage);
            return (Void) null;
        }, mainThreadExecutor);
    }

    @Override
    public CompletableFuture<Void> withdraw(UUID playerUuid, double amount) {
        if (amount <= 0) return CompletableFuture.failedFuture(
                new HCFException("La cantidad debe ser mayor a 0"));
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            if (!economy.has(player, amount))
                throw new InsufficientFundsException(amount, economy.getBalance(player));
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            if (!response.transactionSuccess())
                throw new HCFException("Error al retirar: " + response.errorMessage);
            return (Void) null;
        }, mainThreadExecutor);
    }

    @Override
    public CompletableFuture<Void> transfer(UUID fromUuid, UUID toUuid, double amount) {
        return withdraw(fromUuid, amount).thenCompose(v -> deposit(toUuid, amount));
    }

    @Override
    public CompletableFuture<Boolean> hasEnoughBalance(UUID playerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            return economy.has(player, amount);
        }, mainThreadExecutor);
    }
}
