package dev.alexissdev.kronos.api.exception;

/**
 * Root runtime exception for errors produced within the Kronos public API layer.
 * <p>
 * Thrown when an unrecoverable error occurs during the execution of an operation
 * exposed by the {@code kronos-api} facades, such as failures in communication with
 * internal services or unexpected states in the HCF system.
 * </p>
 * <p>
 * Because it extends {@link RuntimeException}, it does not need to be declared or
 * explicitly caught by API consumers; however, handling it at integration boundaries
 * is strongly recommended to prevent uncontrolled propagation into Bukkit.
 * </p>
 */
public class HCFApiException extends RuntimeException {

    /**
     * Construye una nueva excepción con el mensaje descriptivo indicado.
     *
     * @param message descripción legible del error ocurrido dentro de la API
     */
    public HCFApiException(String message) {
        super(message);
    }

    /**
     * Construye una nueva excepción con un mensaje descriptivo y la causa original del fallo.
     * <p>
     * Útil para envolver excepciones internas (por ejemplo, fallos de MongoDB o Redis)
     * sin exponer detalles de implementación a los consumidores de la API.
     * </p>
     *
     * @param message descripción legible del error ocurrido dentro de la API
     * @param cause   excepción original que provocó este fallo
     */
    public HCFApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
