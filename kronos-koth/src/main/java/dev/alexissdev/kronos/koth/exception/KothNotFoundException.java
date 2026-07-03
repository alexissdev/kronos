package dev.alexissdev.kronos.koth.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

/**
 * Excepción lanzada cuando se intenta operar sobre un KOTH que no existe en el repositorio.
 *
 * <p>Extiende {@link HCFException} para integrarse con el manejo de errores centralizado
 * del plugin Kronos. Es lanzada por {@code KothApplicationService} en operaciones como
 * {@code startKoth}, {@code endKoth} y {@code captureKoth} cuando el nombre proporcionado
 * no corresponde a ninguna zona registrada.</p>
 */
public class KothNotFoundException extends HCFException {

    /**
     * Construye la excepción con un mensaje descriptivo que incluye el nombre del KOTH buscado.
     *
     * @param name nombre del KOTH que no fue encontrado en la base de datos
     */
    public KothNotFoundException(String name) {
        super("KOTH not found: " + name);
    }
}
