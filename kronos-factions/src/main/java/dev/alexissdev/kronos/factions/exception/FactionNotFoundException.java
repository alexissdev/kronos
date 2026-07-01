package dev.alexissdev.kronos.factions.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

public class FactionNotFoundException extends HCFException {

    public FactionNotFoundException(String identifier) {
        super("Faction not found: " + identifier);
    }
}
