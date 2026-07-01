package dev.alexissdev.kronos.core.exception;

import dev.alexissdev.kronos.core.domain.FactionRole;

public class FactionPermissionException extends HCFException {

    public FactionPermissionException(FactionRole required) {
        super("Necesitas rango " + required.name() + " o superior para esto");
    }

    public FactionPermissionException(java.util.UUID actorUuid) {
        super("No eres miembro de esta facción");
    }
}
