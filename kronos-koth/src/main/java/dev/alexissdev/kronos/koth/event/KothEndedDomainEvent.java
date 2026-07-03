package dev.alexissdev.kronos.koth.event;

/**
 * Evento de dominio publicado vía Guava {@code EventBus} cuando un evento KOTH activo
 * finaliza sin haber sido capturado por ningún jugador.
 *
 * <p>Este evento se distingue de {@code KothCapturedDomainEvent} en que ningún jugador
 * logró permanecer el tiempo necesario en la zona de captura. Los suscriptores típicos
 * son el sistema de scoreboard y el de temporizadores, para detener sus contadores y
 * mostrar el resultado al servidor.</p>
 */
public final class KothEndedDomainEvent {

    private final String kothName;

    /**
     * Construye el evento con el nombre del KOTH que finalizó sin captura.
     *
     * @param kothName nombre único del KOTH cuyo evento terminó
     */
    public KothEndedDomainEvent(String kothName) {
        this.kothName = kothName;
    }

    /**
     * @return nombre único del KOTH cuyo evento ha finalizado sin captura
     */
    public String getKothName() { return kothName; }
}
