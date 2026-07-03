package dev.alexissdev.kronos.common.exception;

/**
 * Excepción base del dominio para el plugin Kronos HCF.
 *
 * <p>Todas las excepciones de negocio del plugin deben extender de esta clase para
 * mantener una jerarquía de errores clara y permitir capturar cualquier error de dominio
 * con un único bloque {@code catch (HCFException e)}.</p>
 *
 * <p>Al ser una {@link RuntimeException}, no obliga a los llamadores a declararla con
 * {@code throws}, lo que simplifica el código en la capa de servicios y comandos donde
 * las excepciones de dominio se manejan en puntos de entrada específicos.</p>
 *
 * <p>Ejemplos de subclases concretas: {@link dev.alexissdev.kronos.economy.exception.InsufficientFundsException}.</p>
 */
public class HCFException extends RuntimeException {

    /**
     * Crea una excepción de dominio HCF con un mensaje descriptivo del error ocurrido.
     *
     * @param message descripción del error de negocio (p. ej. {@code "La cantidad debe ser mayor a 0"})
     */
    public HCFException(String message) {
        super(message);
    }

    /**
     * Crea una excepción de dominio HCF que envuelve una causa técnica subyacente.
     * Útil para convertir excepciones de infraestructura (I/O, base de datos, red)
     * en errores de dominio comprensibles por la capa de aplicación.
     *
     * @param message descripción del error de negocio
     * @param cause   excepción original que provocó este error de dominio
     */
    public HCFException(String message, Throwable cause) {
        super(message, cause);
    }
}
