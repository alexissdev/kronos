package dev.alexissdev.kronos.economy.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

/**
 * Excepción de dominio que se lanza cuando un jugador intenta realizar una operación económica
 * pero no dispone de fondos suficientes en su cuenta para completarla.
 *
 * <p>Extiende {@link dev.alexissdev.kronos.common.exception.HCFException} para integrarse
 * en la jerarquía de excepciones del plugin Kronos HCF. El mensaje de error incluye
 * tanto la cantidad requerida como la disponible, facilitando la depuración y la
 * construcción de mensajes descriptivos para el jugador.</p>
 *
 * <p>Esta excepción es lanzada por {@link dev.alexissdev.kronos.economy.VaultEconomyService}
 * durante las operaciones de retiro ({@code withdraw}) y transferencia ({@code transfer})
 * cuando el saldo del jugador es insuficiente.</p>
 */
public class InsufficientFundsException extends HCFException {

    /**
     * Crea una excepción de fondos insuficientes con la información exacta del déficit económico.
     *
     * @param required  cantidad de dinero que se necesitaba para completar la operación
     * @param available cantidad de dinero que el jugador tenía disponible en su cuenta
     */
    public InsufficientFundsException(double required, double available) {
        super(String.format("Insufficient funds: required %.2f but available %.2f", required, available));
    }
}
