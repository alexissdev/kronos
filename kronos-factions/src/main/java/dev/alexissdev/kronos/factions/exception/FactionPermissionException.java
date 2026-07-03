package dev.alexissdev.kronos.factions.exception;

import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.domain.FactionRole;

import java.util.UUID;

/**
 * Excepción lanzada cuando un jugador intenta ejecutar una operación de facción
 * para la que no tiene el rango mínimo requerido, o cuando el actor no es
 * miembro de la facción en cuestión.
 *
 * <p>Esta excepción es producida por el método {@code requireRole} interno del
 * servicio de facciones, que actúa como guardián de permisos antes de cualquier
 * operación privilegiada (expulsar, retirar fondos, cambiar roles, etc.).
 *
 * <p>El mensaje de error es legible por el jugador y se muestra directamente
 * en el chat como parte del manejo estándar de {@code HCFException}.
 */
public class FactionPermissionException extends HCFException {

    /**
     * Crea la excepción indicando el rango mínimo necesario para la operación.
     *
     * @param required rol mínimo requerido que el actor no posee
     */
    public FactionPermissionException(FactionRole required) {
        super("Necesitas rango " + required.name() + " o superior para esto");
    }

    /**
     * Crea la excepción cuando el actor no es miembro de la facción objetivo.
     *
     * @param actorUuid UUID del jugador que intentó la operación sin ser miembro
     */
    public FactionPermissionException(UUID actorUuid) {
        super("No eres miembro de esta facción");
    }
}
