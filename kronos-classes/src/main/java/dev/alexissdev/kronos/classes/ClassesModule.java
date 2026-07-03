package dev.alexissdev.kronos.classes;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.classes.listener.ClassListener;
import dev.alexissdev.kronos.classes.service.KitService;

/**
 * Módulo Guice que configura las dependencias del subsistema de clases (kits) HCF.
 *
 * <p>Registra las siguientes vinculaciones en el contenedor de inyección de dependencias:</p>
 * <ul>
 *   <li>{@link KitService} → {@link KitApplicationService} (singleton): servicio de
 *       aplicación que gestiona la detección, activación y actualización del kit activo
 *       de cada jugador.</li>
 *   <li>{@link dev.alexissdev.kronos.classes.listener.ClassListener} (singleton): listener de
 *       Bukkit que aplica las habilidades pasivas y activas de cada clase durante el juego.</li>
 * </ul>
 *
 * <p>Este módulo debe instalarse en el injector principal del plugin durante la
 * inicialización de Kronos.</p>
 */
public class ClassesModule extends AbstractModule {

    /**
     * Declara todas las vinculaciones de tipos del módulo de clases/kits.
     *
     * <p>Guice llamará a este método automáticamente al construir el injector. No debe
     * invocarse directamente desde código de aplicación.</p>
     */
    @Override
    protected void configure() {
        bind(KitApplicationService.class).in(Singleton.class);
        bind(KitService.class).to(KitApplicationService.class);

        bind(ClassListener.class).in(Singleton.class);
    }
}
