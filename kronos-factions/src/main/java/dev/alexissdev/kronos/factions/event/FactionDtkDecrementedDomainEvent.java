package dev.alexissdev.kronos.factions.event;

/**
 * Evento de dominio publicado en el {@code EventBus} cada vez que el contador
 * DTK (Deaths To Kick) de una facción disminuye como consecuencia de la muerte
 * de uno de sus miembros.
 *
 * <p>Este evento se genera <strong>siempre</strong> que un miembro muere y aún
 * quedan DTK por consumir, por lo que puede usarse para enviar alertas progresivas
 * a los miembros de la facción (p.ej. "¡Cuidado! Solo quedan 5 DTK").
 *
 * <p>Si tras el decremento el DTK llega a 0, el sistema también publicará
 * {@link FactionRaidableDomainEvent} en la misma transacción.
 */
public class FactionDtkDecrementedDomainEvent {

    private final String factionId;
    private final String factionName;
    private final int newDtk;
    private final int maxDtk;

    /**
     * Crea el evento con el estado actualizado del DTK de la facción.
     *
     * @param factionId   ID de la facción cuyo DTK fue decrementado
     * @param factionName nombre de la facción
     * @param newDtk      valor de DTK restante tras el decremento
     * @param maxDtk      valor máximo de DTK configurado para la facción
     */
    public FactionDtkDecrementedDomainEvent(String factionId, String factionName, int newDtk, int maxDtk) {
        this.factionId   = factionId;
        this.factionName = factionName;
        this.newDtk      = newDtk;
        this.maxDtk      = maxDtk;
    }

    /**
     * Devuelve el ID de la facción afectada.
     *
     * @return ID de la facción
     */
    public String getFactionId()   { return factionId; }

    /**
     * Devuelve el nombre de la facción afectada.
     *
     * @return nombre de la facción
     */
    public String getFactionName() { return factionName; }

    /**
     * Devuelve el nuevo valor de DTK restante tras el decremento.
     *
     * <p>Un valor de 0 indica que la facción ha quedado en estado raideable.
     *
     * @return DTK restantes actualizados
     */
    public int    getNewDtk()      { return newDtk; }

    /**
     * Devuelve el número máximo de DTK configurado para esta facción.
     *
     * <p>Útil para calcular el porcentaje de DTK restante y mostrar barras de progreso.
     *
     * @return DTK máximo
     */
    public int    getMaxDtk()      { return maxDtk; }
}
