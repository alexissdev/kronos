package dev.alexissdev.kronos.koth.event;

import java.util.UUID;

/**
 * Evento de dominio publicado vía Guava {@code EventBus} cuando un jugador captura exitosamente
 * un evento KOTH al permanecer el tiempo requerido en la zona de captura.
 *
 * <p>Los suscriptores de este evento (p.ej. el sistema de recompensas) pueden leer el UUID
 * del captor para entregar el cofre correspondiente al tipo de recompensa configurado en
 * la {@code KothZone}.</p>
 */
public final class KothCapturedDomainEvent {

    private final String kothName;
    private final UUID captorUuid;

    /**
     * Construye el evento con los datos del KOTH capturado y el captor.
     *
     * @param kothName    nombre único del KOTH que fue capturado
     * @param captorUuid  UUID del jugador que realizó la captura
     */
    public KothCapturedDomainEvent(String kothName, UUID captorUuid) {
        this.kothName = kothName;
        this.captorUuid = captorUuid;
    }

    /**
     * @return nombre único del KOTH que fue capturado
     */
    public String getKothName() { return kothName; }

    /**
     * @return UUID del jugador que capturó el KOTH y debe recibir la recompensa
     */
    public UUID getCaptorUuid() { return captorUuid; }
}
