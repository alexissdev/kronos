package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener de Bukkit responsable de gestionar el ciclo de vida de los scoreboards
 * individuales de cada jugador en el plugin HCF Kronos.
 * <p>
 * Cuando un jugador entra al servidor, crea su marcador lateral mediante
 * {@link ScoreboardManager#createBoard(org.bukkit.entity.Player)}.
 * Cuando abandona la sesión, libera los recursos asociados mediante
 * {@link ScoreboardManager#removeBoard(org.bukkit.entity.Player)}.
 * </p>
 * <p>
 * Ambos eventos se escuchan con prioridad {@link EventPriority#MONITOR} para
 * garantizar que los demás plugins hayan procesado el evento antes de que
 * este listener actúe.
 * </p>
 */
@Singleton
public class ScoreboardListener implements Listener {

    private final ScoreboardManager manager;

    /**
     * Construye el listener inyectando el gestor central de scoreboards.
     *
     * @param manager gestor que administra los {@link PlayerBoard} de todos los jugadores
     */
    @Inject
    public ScoreboardListener(ScoreboardManager manager) {
        this.manager = manager;
    }

    /**
     * Crea el marcador lateral para el jugador que acaba de conectarse al servidor.
     * <p>
     * Se ejecuta con prioridad {@link EventPriority#MONITOR} para asegurarse de que
     * el jugador ya fue validado y aceptado por el resto de listeners antes de asignarle
     * el scoreboard.
     * </p>
     *
     * @param event evento de conexión del jugador emitido por Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        manager.createBoard(event.getPlayer());
    }

    /**
     * Elimina el marcador lateral y los datos cacheados del jugador que abandona el servidor,
     * liberando la memoria asociada en el {@link ScoreboardManager}.
     *
     * @param event evento de desconexión del jugador emitido por Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.removeBoard(event.getPlayer());
    }
}
