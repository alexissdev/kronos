package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bukkit listener responsible for managing the lifecycle of each player's individual
 * scoreboard in the HCF Kronos plugin.
 * <p>
 * When a player joins the server, it creates their sidebar scoreboard via
 * {@link ScoreboardManager#createBoard(org.bukkit.entity.Player)}.
 * When they leave the session, it releases the associated resources via
 * {@link ScoreboardManager#removeBoard(org.bukkit.entity.Player)}.
 * </p>
 * <p>
 * Both events are listened to at {@link EventPriority#MONITOR} priority to
 * ensure all other plugins have processed the event before this listener acts.
 * </p>
 */
@Singleton
public class ScoreboardListener implements Listener {

    private final ScoreboardManager manager;

    /**
     * Constructs the listener by injecting the central scoreboard manager.
     *
     * @param manager the manager that administers the {@link PlayerBoard} for every online player
     */
    @Inject
    public ScoreboardListener(ScoreboardManager manager) {
        this.manager = manager;
    }

    /**
     * Creates the sidebar scoreboard for a player who has just connected to the server.
     * <p>
     * Runs at {@link EventPriority#MONITOR} priority to ensure the player has already
     * been validated and accepted by all other listeners before the scoreboard is assigned.
     * </p>
     *
     * @param event the player join event emitted by Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        manager.createBoard(event.getPlayer());
    }

    /**
     * Removes the sidebar scoreboard and cached data of a player who is leaving the server,
     * freeing the memory held by their entry in the {@link ScoreboardManager}.
     *
     * @param event the player quit event emitted by Bukkit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.removeBoard(event.getPlayer());
    }
}
