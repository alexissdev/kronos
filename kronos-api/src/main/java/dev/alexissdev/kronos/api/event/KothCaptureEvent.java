package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Bukkit event fired when a player successfully captures a KOTH (King of The Hill) zone.
 * <p>
 * A capture occurs when a player remains inside the KOTH zone for the required amount
 * of time without being eliminated by competing players. Upon capturing, the player
 * or their faction receives the configured reward (typically a loot crate).
 * </p>
 * <p>
 * This event is not cancellable because the capture has already been processed and the
 * reward delivered by the time it is fired. Listeners can use it to record statistics,
 * announce captures to external channels, or update leaderboards.
 * </p>
 */
public class KothCaptureEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String kothName;
    private final UUID captorUuid;

    /**
     * Construye un nuevo evento de captura de zona KOTH.
     *
     * @param kothName   nombre identificador de la zona KOTH que fue capturada
     * @param captorUuid UUID del jugador que completó la captura y recibió la recompensa
     */
    public KothCaptureEvent(String kothName, UUID captorUuid) {
        this.kothName = kothName;
        this.captorUuid = captorUuid;
    }

    /**
     * Retorna el nombre de la zona KOTH que fue capturada.
     * <p>
     * Este nombre puede usarse para consultar detalles adicionales de la zona
     * a través de {@code KothApi#getKoth(String)}.
     * </p>
     *
     * @return nombre de la zona KOTH capturada
     */
    public String getKothName() { return kothName; }

    /**
     * Retorna el UUID del jugador que realizó la captura de la zona KOTH.
     * <p>
     * Con este UUID se puede determinar a qué facción pertenece el captor consultando
     * {@code FactionApi#getByPlayer(UUID)}, lo que permite calcular recompensas
     * a nivel de facción.
     * </p>
     *
     * @return UUID del jugador que capturó el KOTH
     */
    public UUID getCaptorUuid() { return captorUuid; }

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
