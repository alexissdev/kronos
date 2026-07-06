package dev.alexissdev.kronos.scoreboard;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module that configures the scoreboard subsystem of the Kronos HCF plugin.
 * <p>
 * Binds all sidebar scoreboard components as singletons:
 * the renderer ({@link ScoreboardRenderer}), the central manager ({@link ScoreboardManager}),
 * the Bukkit listener ({@link ScoreboardListener}), and the periodic task ({@link ScoreboardTask}).
 * Because they are singletons, Guice guarantees that exactly one instance of each class
 * exists for the entire lifecycle of the plugin, preventing duplicate initialisation
 * and race conditions.
 * </p>
 * <p>
 * This module must be installed into the plugin's main injector alongside the other
 * project modules (factions, economy, timers, koth, etc.).
 * </p>
 */
public class ScoreboardModule extends AbstractModule {

    /**
     * Configures the bindings for the scoreboard subsystem.
     * <p>
     * All components are bound in {@link Singleton} scope to ensure a single shared
     * instance. The injection order is managed automatically by Guice through
     * its dependency graph.
     * </p>
     */
    @Override
    protected void configure() {
        bind(ScoreboardRenderer.class).in(Singleton.class);
        bind(ScoreboardManager.class).in(Singleton.class);
        bind(ScoreboardListener.class).in(Singleton.class);
        bind(ScoreboardTask.class).in(Singleton.class);
    }
}
