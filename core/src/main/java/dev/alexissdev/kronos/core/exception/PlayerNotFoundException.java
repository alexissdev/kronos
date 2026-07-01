package dev.alexissdev.kronos.core.exception;

import java.util.UUID;

public class PlayerNotFoundException extends HCFException {

    public PlayerNotFoundException(UUID uuid) {
        super("Player not found: " + uuid);
    }

    public PlayerNotFoundException(String name) {
        super("Player not found: " + name);
    }
}
