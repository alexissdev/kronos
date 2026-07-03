package dev.alexissdev.kronos.economy.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato del servicio de economía del plugin Kronos HCF.
 *
 * <p>Define las operaciones fundamentales para gestionar el dinero de los jugadores:
 * consultar saldo, acreditar, debitar, transferir y verificar fondos. Todas las
 * operaciones devuelven {@link CompletableFuture} para permitir su ejecución de forma
 * asíncrona sin bloquear el hilo principal del servidor.</p>
 *
 * <p>La implementación concreta es {@link dev.alexissdev.kronos.economy.VaultEconomyService},
 * que delega las operaciones a Vault y al plugin de economía instalado en el servidor.
 * Usar esta interfaz como tipo de dependencia (en lugar de la implementación directa)
 * permite cambiar el backend económico sin modificar los consumidores del servicio.</p>
 */
public interface EconomyService {

    /**
     * Consulta el saldo actual de un jugador de forma asíncrona.
     *
     * @param playerUuid UUID del jugador cuyo saldo se desea conocer
     * @return futuro que se completa con el saldo del jugador expresado en la moneda del servidor
     */
    CompletableFuture<Double> getBalance(UUID playerUuid);

    /**
     * Acredita una cantidad de dinero en la cuenta de un jugador.
     *
     * @param playerUuid UUID del jugador beneficiario del depósito
     * @param amount     cantidad de dinero a acreditar; debe ser mayor que 0
     * @return futuro vacío que se completa cuando el depósito fue procesado exitosamente
     * @throws dev.alexissdev.kronos.common.exception.HCFException si la cantidad no es válida
     *         o si el proveedor de economía reporta un error en la transacción
     */
    CompletableFuture<Void> deposit(UUID playerUuid, double amount);

    /**
     * Debita una cantidad de dinero de la cuenta de un jugador.
     *
     * @param playerUuid UUID del jugador al que se le debitará el dinero
     * @param amount     cantidad de dinero a debitar; debe ser mayor que 0
     * @return futuro vacío que se completa cuando el retiro fue procesado exitosamente
     * @throws dev.alexissdev.kronos.economy.exception.InsufficientFundsException si el jugador
     *         no tiene saldo suficiente para cubrir la cantidad solicitada
     * @throws dev.alexissdev.kronos.common.exception.HCFException si la cantidad no es válida
     *         o si el proveedor de economía reporta un error en la transacción
     */
    CompletableFuture<Void> withdraw(UUID playerUuid, double amount);

    /**
     * Transfiere dinero de la cuenta de un jugador a la cuenta de otro de forma atómica.
     * Si el retiro del origen falla, el depósito al destino no se ejecuta.
     *
     * @param fromUuid UUID del jugador que envía el dinero
     * @param toUuid   UUID del jugador que recibe el dinero
     * @param amount   cantidad a transferir; debe ser mayor que 0
     * @return futuro vacío que se completa cuando ambas operaciones (retiro y depósito) fueron exitosas
     * @throws dev.alexissdev.kronos.economy.exception.InsufficientFundsException si el jugador origen
     *         no tiene fondos suficientes
     * @throws dev.alexissdev.kronos.common.exception.HCFException si ocurre cualquier otro error
     *         durante la transferencia
     */
    CompletableFuture<Void> transfer(UUID fromUuid, UUID toUuid, double amount);

    /**
     * Verifica si un jugador dispone de al menos la cantidad indicada en su cuenta.
     * Útil para realizar validaciones previas sin intentar el retiro directamente.
     *
     * @param playerUuid UUID del jugador a verificar
     * @param amount     cantidad mínima requerida
     * @return futuro que se completa con {@code true} si el jugador tiene saldo suficiente;
     *         {@code false} en caso contrario
     */
    CompletableFuture<Boolean> hasEnoughBalance(UUID playerUuid, double amount);
}
