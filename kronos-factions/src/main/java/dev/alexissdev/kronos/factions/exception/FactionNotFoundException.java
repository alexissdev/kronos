package dev.alexissdev.kronos.factions.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

/**
 * Exception thrown when an operation is attempted on a faction that does not exist
 * in the faction repository.
 *
 * <p>Typically thrown in faction service methods when a lookup by ID or name returns
 * an empty {@link java.util.Optional} and the operation requires the faction to exist
 * (e.g. disband, rename, invite members).
 *
 * <p>As it extends {@code HCFException}, the error message is suitable for displaying
 * directly to the player in the chat.
 */
public class FactionNotFoundException extends HCFException {

    /**
     * Creates the exception indicating the identifier of the faction that was not found.
     *
     * @param identifier ID or name of the faction that could not be located
     */
    public FactionNotFoundException(String identifier) {
        super("Faction not found: " + identifier);
    }
}
