package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Bukkit event fired when a faction is disbanded in the HCF system.
 * <p>
 * Disbanding may be initiated voluntarily by the faction leader, or forcibly by a
 * server administrator. This event is not cancellable because by the time it is fired,
 * the faction has already been removed from the database and its territories have been
 * released back to Wilderness.
 * </p>
 * <p>
 * Listeners of this event can use it to clean up faction-related data in external plugins,
 * such as score tables, leaderboards, or region permission mappings.
 * </p>
 */
public class FactionDisbandEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String factionId;
    private final String factionName;
    private final UUID actorUuid;

    /**
     * Construye un nuevo evento de disolución de facción.
     *
     * @param factionId   identificador único de la facción que fue disuelta
     * @param factionName nombre de la facción en el momento de su disolución
     * @param actorUuid   UUID del jugador que inició la disolución; puede ser el líder
     *                    o un administrador con permisos de moderación
     */
    public FactionDisbandEvent(String factionId, String factionName, UUID actorUuid) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.actorUuid = actorUuid;
    }

    /**
     * Retorna el identificador único de la facción que fue disuelta.
     * <p>
     * Dado que la facción ya no existe en el sistema al momento de este evento,
     * este ID no puede ser consultado a través de {@code FactionApi}.
     * </p>
     *
     * @return ID interno de la facción disuelta
     */
    public String getFactionId() { return factionId; }

    /**
     * Retorna el nombre que tenía la facción en el momento de su disolución.
     * <p>
     * Útil para notificaciones al servidor o registros de auditoría.
     * </p>
     *
     * @return nombre de la facción disuelta
     */
    public String getFactionName() { return factionName; }

    /**
     * Retorna el UUID del jugador que ejecutó la acción de disolución.
     * <p>
     * Puede corresponder al líder de la facción o a un administrador del servidor
     * que forzó la disolución mediante comandos de moderación.
     * </p>
     *
     * @return UUID del actor responsable de la disolución
     */
    public UUID getActorUuid() { return actorUuid; }

    /**
     * Retorna la lista de handlers registrados para este evento, requerida por Bukkit.
     *
     * @return {@link HandlerList} de instancia de este evento
     */
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    /**
     * Retorna la lista global de handlers requerida por el sistema de eventos de Bukkit.
     * <p>
     * Bukkit invoca este método estático mediante reflexión para registrar listeners.
     * </p>
     *
     * @return {@link HandlerList} estática compartida de este tipo de evento
     */
    public static HandlerList getHandlerList() { return HANDLERS; }
}
