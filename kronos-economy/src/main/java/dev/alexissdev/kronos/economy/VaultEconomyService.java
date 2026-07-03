package dev.alexissdev.kronos.economy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.economy.exception.InsufficientFundsException;
import dev.alexissdev.kronos.economy.service.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Implementación de {@link EconomyService} basada en Vault para el plugin Kronos HCF.
 *
 * <p>Delega todas las operaciones de economía (consultas, depósitos, retiros y transferencias)
 * a la API de {@link Economy} de Vault, que a su vez las redirige al plugin de economía
 * concreto instalado en el servidor (p. ej. EssentialsX, CMI).</p>
 *
 * <p>La API de Vault solo puede llamarse de forma segura desde el hilo principal de Bukkit.
 * Por ello, todas las operaciones se ejecutan a través de {@code mainThreadExecutor}, un
 * {@link java.util.concurrent.Executor} personalizado que detecta si la llamada ya proviene
 * del hilo principal o la programa mediante el scheduler de Bukkit. Todas las operaciones
 * devuelven {@link CompletableFuture} para no bloquear el hilo del llamador.</p>
 *
 * <p>Gestionada como {@code @Singleton} por Guice; se inyecta en los servicios y comandos
 * que necesiten interactuar con la economía del servidor.</p>
 */
@Singleton
public class VaultEconomyService implements EconomyService {

    private final Economy economy;
    private final Executor mainThreadExecutor;

    /**
     * Construye el servicio de economía y configura el executor del hilo principal de Bukkit.
     *
     * <p>El {@code mainThreadExecutor} garantiza que todas las llamadas a la API de Vault
     * se realicen en el hilo principal, independientemente del hilo que invoque las operaciones
     * asíncronas de este servicio.</p>
     *
     * @param economy instancia de {@link Economy} de Vault inyectada por Guice
     * @param plugin  instancia del plugin principal, necesaria para programar tareas en el scheduler de Bukkit
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>La consulta del saldo se ejecuta en el hilo principal de Bukkit ya que la API
     * de Vault no es thread-safe.</p>
     *
     * @param playerUuid UUID del jugador cuyo saldo se desea consultar
     * @return futuro que se completará con el saldo del jugador expresado como {@code double}
     */
    @Override
    public CompletableFuture<Double> getBalance(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            return economy.getBalance(player);
        }, mainThreadExecutor);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Valida que la cantidad sea positiva antes de ejecutar la operación. Si Vault reporta
     * un error en la transacción, el futuro se completa excepcionalmente con una {@link HCFException}.</p>
     *
     * @param playerUuid UUID del jugador al que se le acreditará el dinero
     * @param amount     cantidad a depositar; debe ser mayor que 0
     * @return futuro vacío que se completa cuando el depósito fue exitoso
     * @throws HCFException si la cantidad es menor o igual a cero, o si Vault reporta un error
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Verifica que el jugador tenga fondos suficientes antes de intentar el retiro.
     * Si no los tiene, el futuro se completa excepcionalmente con {@link InsufficientFundsException}.</p>
     *
     * @param playerUuid UUID del jugador al que se le debitará el dinero
     * @param amount     cantidad a retirar; debe ser mayor que 0
     * @return futuro vacío que se completa cuando el retiro fue exitoso
     * @throws HCFException              si la cantidad es menor o igual a cero, o si Vault reporta un error
     * @throws InsufficientFundsException si el jugador no tiene saldo suficiente para cubrir el retiro
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>La transferencia se implementa como dos operaciones atómicas en cadena:
     * primero un retiro del origen y, si tiene éxito, un depósito al destino.
     * Si el retiro falla (fondos insuficientes, error de Vault), el depósito no se ejecuta.</p>
     *
     * @param fromUuid UUID del jugador que envía el dinero
     * @param toUuid   UUID del jugador que recibe el dinero
     * @param amount   cantidad a transferir; debe ser mayor que 0
     * @return futuro vacío que se completa cuando ambas operaciones fueron exitosas
     * @throws InsufficientFundsException si el jugador origen no tiene fondos suficientes
     * @throws HCFException              si ocurre cualquier otro error durante la transferencia
     */
    @Override
    public CompletableFuture<Void> transfer(UUID fromUuid, UUID toUuid, double amount) {
        return withdraw(fromUuid, amount).thenCompose(v -> deposit(toUuid, amount));
    }

    /**
     * {@inheritDoc}
     *
     * @param playerUuid UUID del jugador a verificar
     * @param amount     cantidad mínima requerida
     * @return futuro que se completa con {@code true} si el jugador tiene al menos {@code amount}
     *         en su cuenta; {@code false} en caso contrario
     */
    @Override
    public CompletableFuture<Boolean> hasEnoughBalance(UUID playerUuid, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
            return economy.has(player, amount);
        }, mainThreadExecutor);
    }
}
