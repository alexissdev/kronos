package dev.alexissdev.kronos.claims.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

/**
 * Domain exception thrown when a territory operation violates the HCF claiming rules.
 *
 * <p>Typical scenarios that trigger this exception include:</p>
 * <ul>
 *   <li>Attempting to claim chunks that are already occupied by another claim.</li>
 *   <li>Attempting to unclaim a chunk that does not belong to the requesting faction.</li>
 *   <li>Attempting to overclaim territory owned by the same faction or a non-enemy faction.</li>
 * </ul>
 *
 * <p>By extending {@code HCFException}, the error message is suitable for displaying
 * directly to the player without any additional translation.</p>
 */
public class ClaimConflictException extends HCFException {

    /**
     * Constructs the exception with a descriptive message about the conflict.
     *
     * @param message description of the conflict, suitable for display to the player
     */
    public ClaimConflictException(String message) {
        super(message);
    }
}
