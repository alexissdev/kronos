package dev.alexissdev.kronos.api.exception;

public class HCFApiException extends RuntimeException {

    public HCFApiException(String message) {
        super(message);
    }

    public HCFApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
