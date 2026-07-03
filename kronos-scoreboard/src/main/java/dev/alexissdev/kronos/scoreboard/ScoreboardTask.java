package dev.alexissdev.kronos.scoreboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tarea periódica que gestiona las actualizaciones automáticas de los scoreboards
 * de todos los jugadores conectados al servidor.
 * <p>
 * Al ser instanciada por Guice, registra dos {@link BukkitRunnable} independientes
 * con frecuencias distintas:
 * </p>
 * <ul>
 *   <li><b>Tarea en hilo principal (cada 20 ticks / 1 segundo):</b> redibuja los
 *       conteos de timers activos, los contadores SOTW/EOTW y el estado de los KOTHs
 *       mediante {@link ScoreboardManager#tickAll()}.</li>
 *   <li><b>Tarea asíncrona (cada 100 ticks / 5 segundos):</b> refresca las estadísticas
 *       lentas —kills, deaths, balance económico y facción— sin bloquear el hilo del
 *       servidor, mediante {@link ScoreboardManager#refreshAllStats()}.</li>
 * </ul>
 * <p>
 * Guice instancia esta clase automáticamente como singleton al arrancar el plugin
 * a través de {@link ScoreboardModule}.
 * </p>
 */
@Singleton
public class ScoreboardTask {

    /**
     * Registra las tareas periódicas de actualización de scoreboards en el scheduler de Bukkit.
     * <p>
     * La tarea de redibujado se inicia con un retardo de 20 ticks y se repite cada 20 ticks
     * (1 segundo) en el hilo principal. La tarea de refresco de estadísticas se inicia
     * con un retardo de 40 ticks y se repite cada 100 ticks (5 segundos) de forma asíncrona.
     * </p>
     *
     * @param plugin  instancia principal del plugin, necesaria para acceder al scheduler de Bukkit
     * @param manager gestor central del sistema de scoreboards que provee los métodos de actualización
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
