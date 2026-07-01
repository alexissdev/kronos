package dev.alexissdev.kronos.players.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

import java.util.UUID;

public class PlayerNotFoundException extends HCFException {
    public PlayerNotFoundException(String message) {
        super(message);
    }

    public PlayerNotFoundException(UUID uuid) {
        super("Player not found: " + uuid);
    }
}
