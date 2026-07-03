package dev.alexissdev.kronos.spawn;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.spawn.command.SpawnCommand;
import dev.alexissdev.kronos.spawn.creation.SpawnCreationService;
import dev.alexissdev.kronos.spawn.listener.SpawnListener;
import dev.alexissdev.kronos.spawn.listener.SpawnWandListener;
import dev.alexissdev.kronos.spawn.persistence.MongoSpawnRepository;
import dev.alexissdev.kronos.spawn.repository.SpawnRepository;
import dev.alexissdev.kronos.spawn.service.SpawnService;

/**
 * Módulo de inyección de dependencias Guice para el subsistema de spawn del plugin Kronos HCF.
 *
 * <p>Registra todas las dependencias del módulo de spawn en el contenedor de Guice:
 * <ul>
 *   <li>{@code SpawnApplicationService} — implementación singleton del servicio de negocio.</li>
 *   <li>{@code SpawnService} — interfaz del servicio, enlazada a {@code SpawnApplicationService}.</li>
 *   <li>{@code SpawnRepository} — interfaz del repositorio, enlazada a {@code MongoSpawnRepository}.</li>
 *   <li>{@code SpawnCreationService} — servicio que gestiona las sesiones de creación de zona.</li>
 *   <li>{@code SpawnCommand} — comando administrativo {@code /spawn}.</li>
 *   <li>{@code SpawnListener} — listener que gestiona entradas/salidas del spawn y timers de PvP.</li>
 *   <li>{@code SpawnWandListener} — listener que procesa los clics de la varita de selección.</li>
 * </ul>
 *
 * <p>Este módulo debe ser instalado en el inyector principal del plugin durante la inicialización.</p>
 */
public class SpawnModule extends AbstractModule {

    /**
     * Configura los bindings de Guice para todas las clases del módulo de spawn.
     * Todas las instancias se registran como singletons para mantener el estado compartido.
     */
    @Override
    protected void configure() {
        bind(SpawnApplicationService.class).in(Singleton.class);
        bind(SpawnService.class).to(SpawnApplicationService.class);
        bind(SpawnRepository.class).to(MongoSpawnRepository.class).in(Singleton.class);
        bind(SpawnCreationService.class).in(Singleton.class);
        bind(SpawnCommand.class).in(Singleton.class);
        bind(SpawnListener.class).in(Singleton.class);
        bind(SpawnWandListener.class).in(Singleton.class);
    }
}
