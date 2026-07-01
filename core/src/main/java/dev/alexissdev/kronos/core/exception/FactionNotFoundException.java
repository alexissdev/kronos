package dev.alexissdev.kronos.core.exception;

public class FactionNotFoundException extends HCFException {

    public FactionNotFoundException(String identifier) {
        super("Faction not found: " + identifier);
    }
}
