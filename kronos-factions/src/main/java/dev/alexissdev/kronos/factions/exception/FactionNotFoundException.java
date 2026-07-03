package dev.alexissdev.kronos.factions.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

/**
 * Excepción lanzada cuando se intenta operar sobre una facción que no existe
 * en el repositorio de facciones.
 *
 * <p>Se lanza típicamente en los métodos del servicio de facciones cuando la
 * búsqueda por ID o nombre devuelve un {@link java.util.Optional} vacío y la
 * operación requiere que la facción exista (p.ej. disolver, renombrar, invitar miembros).
 *
 * <p>Como extiende {@code HCFException}, el mensaje de error es apto para mostrarse
 * directamente al jugador en el chat.
 */
public class FactionNotFoundException extends HCFException {

    /**
     * Crea la excepción indicando el identificador de la facción no encontrada.
     *
     * @param identifier ID o nombre de la facción que no pudo localizarse
     */
    public FactionNotFoundException(String identifier) {
        super("Faction not found: " + identifier);
    }
}
