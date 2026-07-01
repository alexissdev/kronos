package dev.alexissdev.kronos.core.exception;

public class HCFException extends RuntimeException {

    public HCFException(String message) {
        super(message);
    }

    public HCFException(String message, Throwable cause) {
        super(message, cause);
    }
}
