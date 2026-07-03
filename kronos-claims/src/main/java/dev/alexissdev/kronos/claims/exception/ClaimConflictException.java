package dev.alexissdev.kronos.claims.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

/**
 * Excepción de dominio que se lanza cuando una operación sobre un territorio
 * viola las reglas de reclamación del sistema HCF.
 *
 * <p>Algunos escenarios típicos que la producen son:</p>
 * <ul>
 *   <li>Intentar reclamar chunks que ya están ocupados por otro claim.</li>
 *   <li>Intentar desreclamar un chunk que no pertenece a la propia facción.</li>
 *   <li>Intentar overclamar territorio propio o de una facción no enemiga.</li>
 * </ul>
 *
 * <p>Al extender {@code HCFException}, el mensaje de error es apto para mostrarse
 * directamente al jugador sin necesidad de traducción adicional.</p>
 */
public class ClaimConflictException extends HCFException {

    /**
     * Construye la excepción con un mensaje descriptivo del conflicto.
     *
     * @param message descripción del conflicto, apta para mostrar al jugador
     */
    public ClaimConflictException(String message) {
        super(message);
    }
}
