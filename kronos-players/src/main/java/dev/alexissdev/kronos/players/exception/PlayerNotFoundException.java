package dev.alexissdev.kronos.players.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

import java.util.UUID;

/**
 * Thrown when an attempt is made to access an {@code HCFPlayer} that does not exist
 * in the database.
 *
 * <p>Typically raised during operations that require both players (attacker and victim)
 * to be registered in the system, such as recording kills or applying deathban penalties.</p>
 */
public class PlayerNotFoundException extends HCFException {

    /**
     * Creates the exception with a custom descriptive message.
     *
     * @param message description of the error indicating which player was not found and in what context
     */
    public PlayerNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates the exception from the UUID of the missing player.
     * Automatically generates a message containing the UUID to aid diagnosis.
     *
     * @param uuid UUID of the player that does not exist in the database
     */
    public PlayerNotFoundException(UUID uuid) {
        super("Player not found: " + uuid);
    }
}
