package dev.alexissdev.kronos.economy.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

public class InsufficientFundsException extends HCFException {

    public InsufficientFundsException(double required, double available) {
        super(String.format("Insufficient funds: required %.2f but available %.2f", required, available));
    }
}
