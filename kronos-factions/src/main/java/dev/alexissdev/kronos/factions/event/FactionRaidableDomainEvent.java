package dev.alexissdev.kronos.factions.event;

/**
 * Evento de dominio publicado en el {@code EventBus} cuando una facción se vuelve
 * <em>raideable</em>, es decir, cuando su contador DTK llega a 0.
 *
 * <p>Una facción raideable tiene sus claims vulnerables: otras facciones enemigas
 * pueden sobrereclamar su territorio sin restricciones. Este evento marca el inicio
 * de una "raid" oficial contra la facción afectada.
 *
 * <p>Los listeners típicos de este evento incluyen:
 * <ul>
 *   <li>Anuncios globales al servidor indicando que la facción puede ser raideada.</li>
 *   <li>Actualización de indicadores visuales en el mapa del servidor.</li>
 *   <li>Notificaciones push a los miembros de la facción en peligro.</li>
 * </ul>
 */
public class FactionRaidableDomainEvent {

    private final String factionId;
    private final String factionName;

    /**
     * Crea el evento con los datos de identificación de la facción raideable.
     *
     * @param factionId   ID de la facción que se ha vuelto raideable
     * @param factionName nombre visible de la facción afectada
     */
    public FactionRaidableDomainEvent(String factionId, String factionName) {
        this.factionId   = factionId;
        this.factionName = factionName;
    }

    /**
     * Devuelve el ID de la facción que se ha vuelto raideable.
     *
     * @return ID de la facción
     */
    public String getFactionId()   { return factionId; }

    /**
     * Devuelve el nombre de la facción que se ha vuelto raideable.
     *
     * @return nombre de la facción
     */
    public String getFactionName() { return factionName; }
}
