package dev.alexissdev.kronos.players.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

import java.util.UUID;

/**
 * Excepción lanzada cuando se intenta acceder a un {@code HCFPlayer} que no existe
 * en la base de datos.
 *
 * <p>Se produce típicamente durante operaciones que requieren que ambos jugadores
 * (atacante y víctima) estén registrados en el sistema, como el registro de kills
 * o la aplicación de penalizaciones de deathban.</p>
 */
public class PlayerNotFoundException extends HCFException {

    /**
     * Crea la excepción con un mensaje descriptivo personalizado.
     *
     * @param message descripción del error que indica qué jugador no fue encontrado y en qué contexto
     */
    public PlayerNotFoundException(String message) {
        super(message);
    }

    /**
     * Crea la excepción a partir del UUID del jugador no encontrado.
     * Genera automáticamente un mensaje con el UUID para facilitar el diagnóstico.
     *
     * @param uuid UUID del jugador que no existe en la base de datos
     */
    public PlayerNotFoundException(UUID uuid) {
        super("Player not found: " + uuid);
    }
}
