package dev.alexissdev.kronos.claims.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

public class ClaimConflictException extends HCFException {

    public ClaimConflictException(String message) {
        super(message);
    }
}
