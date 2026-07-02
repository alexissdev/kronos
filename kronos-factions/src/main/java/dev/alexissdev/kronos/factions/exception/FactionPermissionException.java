package dev.alexissdev.kronos.factions.exception;

import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.domain.FactionRole;

import java.util.UUID;

public class FactionPermissionException extends HCFException {

    public FactionPermissionException(FactionRole required) {
        super("Necesitas rango " + required.name() + " o superior para esto");
    }

    public FactionPermissionException(UUID actorUuid) {
        super("No eres miembro de esta facción");
    }
}
