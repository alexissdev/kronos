package dev.alexissdev.kronos.koth;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.koth.command.KothCommand;
import dev.alexissdev.kronos.koth.creation.KothCreationService;
import dev.alexissdev.kronos.koth.listener.KothWandListener;
import dev.alexissdev.kronos.koth.persistence.MongoKothRepository;
import dev.alexissdev.kronos.koth.repository.KothRepository;
import dev.alexissdev.kronos.koth.service.KothService;

/**
 * Módulo de inyección de dependencias Guice para el subsistema KOTH del plugin Kronos HCF.
 *
 * <p>Registra todas las dependencias del módulo KOTH en el contenedor de Guice:
 * <ul>
 *   <li>{@code KothApplicationService} — implementación singleton del servicio de negocio.</li>
 *   <li>{@code KothService} — interfaz del servicio, enlazada a {@code KothApplicationService}.</li>
 *   <li>{@code KothRepository} — interfaz del repositorio, enlazada a {@code MongoKothRepository}.</li>
 *   <li>{@code KothCommand} — comando administrativo {@code /koth}.</li>
 *   <li>{@code KothCreationService} — servicio que gestiona las sesiones de creación de zonas.</li>
 *   <li>{@code KothWandListener} — listener que procesa los clics de la varita de selección.</li>
 * </ul>
 *
 * <p>Este módulo debe ser instalado en el inyector principal del plugin durante la inicialización.</p>
 */
public class KothModule extends AbstractModule {

    /**
     * Configura los bindings de Guice para todas las clases del módulo KOTH.
     * Se establecen todas las instancias como singletons para evitar la creación
     * múltiple de objetos con estado compartido.
     */
    @Override
    protected void configure() {
        bind(KothApplicationService.class).in(Singleton.class);
        bind(KothService.class).to(KothApplicationService.class);
        bind(KothRepository.class).to(MongoKothRepository.class).in(Singleton.class);
        bind(KothCommand.class).in(Singleton.class);
        bind(KothCreationService.class).in(Singleton.class);
        bind(KothWandListener.class).in(Singleton.class);
    }
}
