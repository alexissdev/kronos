package dev.alexissdev.kronos.classes;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.classes.listener.ClassListener;
import dev.alexissdev.kronos.classes.service.KitService;

/**
 * Guice module that configures the dependency bindings for the classes (kits) subsystem.
 *
 * <p>Registers the following bindings in the dependency-injection container:</p>
 * <ul>
 *   <li>{@link KitService} → {@link KitApplicationService} (singleton): application
 *       service that handles the detection, activation, and update of each player's
 *       active kit.</li>
 *   <li>{@link dev.alexissdev.kronos.classes.listener.ClassListener} (singleton): Bukkit
 *       listener that applies the passive and active abilities of each class during
 *       gameplay.</li>
 * </ul>
 *
 * <p>This module must be installed in the plugin's main injector during Kronos
 * initialisation.</p>
 */
public class ClassesModule extends AbstractModule {

    /**
     * Declares all type bindings for the classes/kits module.
     *
     * <p>Guice will call this method automatically when building the injector. It should
     * never be invoked directly from application code.</p>
     */
    @Override
    protected void configure() {
        bind(KitApplicationService.class).in(Singleton.class);
        bind(KitService.class).to(KitApplicationService.class);

        bind(ClassListener.class).in(Singleton.class);
    }
}
