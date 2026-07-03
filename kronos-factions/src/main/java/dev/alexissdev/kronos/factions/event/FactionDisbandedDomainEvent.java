package dev.alexissdev.kronos.factions.event;

import java.util.UUID;

/**
 * Evento de dominio publicado en el {@code EventBus} cuando una facción es disuelta.
 *
 * <p>La disolución puede ocurrir de dos formas:
 * <ul>
 *   <li>El líder ejecuta voluntariamente el comando de disolver la facción.</li>
 *   <li>La facción acumula el número máximo de strikes administrativos.</li>
 * </ul>
 *
 * <p>Los listeners de este evento deben encargarse de limpiar recursos dependientes
 * de la facción, como liberar sus claims, devolver fondos o notificar a los miembros.
 */
public final class FactionDisbandedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final UUID actorUuid;

    /**
     * Crea el evento con los datos de la facción disuelta y el actor responsable.
     *
     * @param factionId   ID de la facción que fue disuelta
     * @param factionName nombre de la facción en el momento de su disolución
     * @param actorUuid   UUID del jugador o entidad que desencadenó la disolución
     *                    (puede ser el líder o un administrador en el caso de strikes)
     */
    public FactionDisbandedDomainEvent(String factionId, String factionName, UUID actorUuid) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.actorUuid = actorUuid;
    }

    /**
     * Devuelve el ID de la facción que fue disuelta.
     *
     * @return ID de la facción
     */
    public String getFactionId() { return factionId; }

    /**
     * Devuelve el nombre que tenía la facción antes de ser disuelta.
     *
     * @return nombre de la facción
     */
    public String getFactionName() { return factionName; }

    /**
     * Devuelve el UUID del jugador o entidad que provocó la disolución.
     *
     * @return UUID del actor responsable
     */
    public UUID getActorUuid() { return actorUuid; }
}
