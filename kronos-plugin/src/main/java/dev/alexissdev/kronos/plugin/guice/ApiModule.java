package dev.alexissdev.kronos.plugin.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.HCFApi;
import dev.alexissdev.kronos.api.HCFApiImpl;

/**
 * Módulo Guice que expone la API pública del plugin HCF.
 *
 * <p>Vincula {@link HCFApi} a su implementación {@link HCFApiImpl} como singleton, permitiendo
 * que otros módulos o plugins externos obtengan la instancia de la API a través de Bukkit
 * {@code ServicesManager} o directamente vía inyección de dependencias.
 */
public class ApiModule extends AbstractModule {

    /**
     * Registra los bindings de la API pública en el contenedor Guice.
     *
     * <p>Tanto {@link HCFApi} (interfaz) como {@link HCFApiImpl} (clase concreta) quedan ligados
     * al mismo singleton, lo que garantiza que cualquier punto de inyección obtenga siempre la
     * misma instancia.
     */
    @Override
    protected void configure() {
        bind(HCFApi.class).to(HCFApiImpl.class).in(Singleton.class);
        bind(HCFApiImpl.class).in(Singleton.class);
    }
}
