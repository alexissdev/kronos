package dev.alexissdev.kronos.api.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.HCFApi;
import dev.alexissdev.kronos.api.HCFApiImpl;

/**
 * Guice module that registers the bindings required for the Kronos public API layer.
 * <p>
 * When installed into the plugin's main injector, it guarantees that {@link HCFApi}
 * resolves to its concrete implementation {@link HCFApiImpl} and that both share the
 * same singleton lifecycle instance throughout the server's runtime.
 * </p>
 * <p>
 * The explicit binding of {@link HCFApiImpl} as a singleton is necessary because Guice
 * does not automatically infer uniqueness when the interface is already bound; this
 * prevents duplicate instances from being created when the concrete implementation is
 * injected directly in internal code.
 * </p>
 */
public class ApiModule extends AbstractModule {

    /**
     * Configura los bindings de la API pública en el inyector Guice.
     * <p>
     * Registra {@link HCFApi} → {@link HCFApiImpl} y fuerza el alcance {@link Singleton}
     * para ambas entradas, asegurando que todas las fachadas internas se inicialicen
     * una única vez y se reutilicen durante el ciclo de vida del plugin.
     * </p>
     */
    @Override
    protected void configure() {
        bind(HCFApi.class).to(HCFApiImpl.class).in(Singleton.class);
        bind(HCFApiImpl.class).in(Singleton.class);
    }
}
