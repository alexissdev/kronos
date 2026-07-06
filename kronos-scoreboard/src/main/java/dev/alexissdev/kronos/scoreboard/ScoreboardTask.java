package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic task that drives the automatic scoreboard updates for all players
 * connected to the server.
 * <p>
 * When instantiated by Guice, it registers two independent {@link BukkitRunnable}
 * instances with different frequencies:
 * </p>
 * <ul>
 *   <li><b>Main-thread task (every 20 ticks / 1 second):</b> redraws active timer
 *       countdowns, SOTW/EOTW counters, and KOTH state via
 *       {@link ScoreboardManager#tickAll()}.</li>
 *   <li><b>Async task (every 100 ticks / 5 seconds):</b> refreshes slow statistics
 *       — kills, deaths, economic balance, and faction — without blocking the server
 *       thread, via {@link ScoreboardManager#refreshAllStats()}.</li>
 * </ul>
 * <p>
 * Guice instantiates this class automatically as a singleton at plugin startup
 * through {@link ScoreboardModule}.
 * </p>
 */
@Singleton
public class ScoreboardTask {

    /**
     * Registers the periodic scoreboard update tasks on Bukkit's scheduler.
     * <p>
     * The redraw task starts with a 20-tick delay and repeats every 20 ticks
     * (1 second) on the main thread. The stats-refresh task starts with a 40-tick
     * delay and repeats every 100 ticks (5 seconds) asynchronously.
     * </p>
     *
     * @param plugin  the main plugin instance, needed to access Bukkit's scheduler
     * @param manager the central scoreboard manager that provides the update methods
     */
    @Inject
    public ScoreboardTask(JavaPlugin plugin, ScoreboardManager manager) {
        // Redraw timer countdowns every second on the main thread.
        new BukkitRunnable() {
            @Override public void run() { manager.tickAll(); }
        }.runTaskTimer(plugin, 20L, 20L);

        // Refresh kills/deaths/faction/balance every 5 seconds async.
        new BukkitRunnable() {
            @Override public void run() { manager.refreshAllStats(); }
        }.runTaskTimerAsynchronously(plugin, 40L, 100L);
    }
}
