package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Singleton
public class ScoreboardTask {

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
