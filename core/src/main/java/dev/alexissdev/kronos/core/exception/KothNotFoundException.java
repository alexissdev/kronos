package dev.alexissdev.kronos.core.exception;

public class KothNotFoundException extends HCFException {

    public KothNotFoundException(String name) {
        super("KOTH not found: " + name);
    }
}
