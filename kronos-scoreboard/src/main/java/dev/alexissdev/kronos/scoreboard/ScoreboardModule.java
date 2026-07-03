package dev.alexissdev.kronos.scoreboard;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Módulo Guice que configura el subsistema de scoreboards del plugin Kronos HCF.
 * <p>
 * Registra como singletons todos los componentes del marcador lateral:
 * el renderizador ({@link ScoreboardRenderer}), el gestor central ({@link ScoreboardManager}),
 * el listener de Bukkit ({@link ScoreboardListener}) y la tarea periódica ({@link ScoreboardTask}).
 * Al ser singletons, Guice garantiza que existe una única instancia de cada clase durante
 * todo el ciclo de vida del plugin, evitando inicializaciones duplicadas y condiciones de carrera.
 * </p>
 * <p>
 * Este módulo debe instalarse en el injector principal del plugin junto con los demás
 * módulos del proyecto (factions, economy, timers, koth, etc.).
 * </p>
 */
public class ScoreboardModule extends AbstractModule {

    /**
     * Configura los bindings del subsistema de scoreboards.
     * <p>
     * Todos los componentes quedan ligados en scope {@link Singleton} para garantizar
     * una única instancia compartida. El orden de inyección es gestionado automáticamente
     * por Guice a través del grafo de dependencias.
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
