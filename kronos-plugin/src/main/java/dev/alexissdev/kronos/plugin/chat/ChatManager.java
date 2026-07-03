package dev.alexissdev.kronos.plugin.chat;

import com.google.inject.Singleton;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Servicio singleton que registra el modo de chat activo de cada jugador en línea.
 *
 * <p>Los modos posibles están definidos en {@link ChatMode}: {@code GLOBAL}, {@code FACTION}
 * y {@code ALLY}. Cuando un jugador no tiene un modo explícito asignado, se asume
 * {@link ChatMode#GLOBAL} como valor predeterminado.
 *
 * <p>La implementación utiliza un {@link ConcurrentHashMap} para soportar accesos concurrentes
 * desde hilos del scheduler de Bukkit sin riesgo de condiciones de carrera.
 */
@Singleton
public class ChatManager {

    private final Map<UUID, ChatMode> modes = new ConcurrentHashMap<>();

    /**
     * Devuelve el modo de chat activo del jugador identificado por el UUID dado.
     *
     * <p>Si el jugador no tiene un modo asignado explícitamente (por ejemplo, porque acaba
     * de conectarse o se reseteó su estado), se devuelve {@link ChatMode#GLOBAL}.
     *
     * @param uuid identificador único del jugador
     * @return el {@link ChatMode} actual del jugador, nunca {@code null}
     */
    public ChatMode getMode(UUID uuid) {
        return modes.getOrDefault(uuid, ChatMode.GLOBAL);
    }

    /**
     * Avanza el modo de chat del jugador al siguiente en el ciclo
     * {@code GLOBAL → FACTION → ALLY → GLOBAL} y devuelve el nuevo modo.
     *
     * <p>Cuando el modo resultante es {@link ChatMode#GLOBAL}, la entrada se elimina del mapa
     * para ahorrar memoria, ya que ese es el valor predeterminado.
     *
     * @param uuid identificador único del jugador
     * @return el nuevo {@link ChatMode} tras el ciclo
     */
    public ChatMode cycleMode(UUID uuid) {
        ChatMode next;
        switch (getMode(uuid)) {
            case GLOBAL:  next = ChatMode.FACTION; break;
            case FACTION: next = ChatMode.ALLY;    break;
            default:      next = ChatMode.GLOBAL;
        }
        if (next == ChatMode.GLOBAL) {
            modes.remove(uuid);
        } else {
            modes.put(uuid, next);
        }
        return next;
    }

    /**
     * Restablece el modo de chat del jugador a {@link ChatMode#GLOBAL} eliminando su entrada del mapa.
     *
     * <p>Debe invocarse al desconectarse un jugador para liberar la memoria asociada a su estado.
     *
     * @param uuid identificador único del jugador cuyo modo se desea restablecer
     */
    public void reset(UUID uuid) {
        modes.remove(uuid);
    }
}
