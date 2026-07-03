package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Evento de dominio publicado en el {@code EventBus} cuando se crea una nueva facción.
 *
 * <p>Los listeners de este evento pueden usarlo para, entre otras cosas, enviar
 * anuncios globales en el servidor, registrar métricas de crecimiento de facciones
 * o inicializar recursos auxiliares asociados a la facción recién creada.
 */
public final class FactionCreatedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final UUID leaderId;

    /**
     * Crea el evento con los datos de la facción recién fundada.
     *
     * @param factionId   ID único asignado a la nueva facción
     * @param factionName nombre visible elegido por el fundador
     * @param leaderId    UUID del jugador que fundó y lidera la facción
     */
    public FactionCreatedDomainEvent(String factionId, String factionName, UUID leaderId) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.leaderId = leaderId;
    }

    /**
     * Devuelve el ID único de la facción creada.
     *
     * @return ID de la facción
     */
    public String getFactionId() { return factionId; }

    /**
     * Devuelve el nombre de la facción creada.
     *
     * @return nombre de la facción
     */
    public String getFactionName() { return factionName; }

    /**
     * Devuelve el UUID del jugador que creó y lidera la facción.
     *
     * @return UUID del líder fundador
     */
    public UUID getLeaderId() { return leaderId; }
}
