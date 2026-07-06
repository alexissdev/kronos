package dev.alexissdev.kronos.factions.exception;

import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.domain.FactionRole;

import java.util.UUID;

/**
 * Exception thrown when a player attempts to execute a faction operation for which
 * they do not hold the minimum required rank, or when the actor is not a member
 * of the target faction at all.
 *
 * <p>This exception is produced by the internal {@code requireRole} method of the
 * faction service, which acts as a permission guard before any privileged operation
 * (kick, withdraw funds, change roles, etc.).
 *
 * <p>The error message is player-readable and is displayed directly in the chat
 * as part of the standard {@code HCFException} handling.
 */
public class FactionPermissionException extends HCFException {

    /**
     * Creates the exception indicating the minimum rank required for the operation.
     *
     * @param required minimum role required that the actor does not possess
     */
    public FactionPermissionException(FactionRole required) {
        super("Necesitas rango " + required.name() + " o superior para esto");
    }

    /**
     * Creates the exception for when the actor is not a member of the target faction.
     *
     * @param actorUuid UUID of the player who attempted the operation without being a member
     */
    public FactionPermissionException(UUID actorUuid) {
        super("No eres miembro de esta facción");
    }
}
