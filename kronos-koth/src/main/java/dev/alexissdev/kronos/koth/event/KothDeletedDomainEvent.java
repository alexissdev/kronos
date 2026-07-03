package dev.alexissdev.kronos.koth.event;

/**
 * Evento de dominio publicado vía Guava {@code EventBus} cuando una zona KOTH es eliminada
 * permanentemente del sistema.
 *
 * <p>Los módulos que mantienen estado asociado a un KOTH (como temporizadores activos o
 * marcadores en el scoreboard) deben suscribirse a este evento para limpiar sus recursos
 * de forma desacoplada.</p>
 */
public final class KothDeletedDomainEvent {

    private final String kothName;

    /**
     * Construye el evento con el nombre del KOTH eliminado.
     *
     * @param kothName nombre único del KOTH que fue eliminado
     */
    public KothDeletedDomainEvent(String kothName) {
        this.kothName = kothName;
    }

    /**
     * @return nombre único del KOTH que fue eliminado del sistema
     */
    public String getKothName() { return kothName; }
}
