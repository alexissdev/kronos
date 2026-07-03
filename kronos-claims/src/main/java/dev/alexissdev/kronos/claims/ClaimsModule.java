package dev.alexissdev.kronos.claims;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.listener.ClaimListener;
import dev.alexissdev.kronos.claims.persistence.MongoClaimRepository;
import dev.alexissdev.kronos.claims.repository.ClaimRepository;
import dev.alexissdev.kronos.claims.service.ClaimService;

/**
 * Módulo Guice que configura las dependencias del subsistema de territorios (claims).
 *
 * <p>Registra las siguientes vinculaciones en el contenedor de inyección de dependencias:</p>
 * <ul>
 *   <li>{@link ClaimService} → {@link ClaimApplicationService} (singleton): servicio de
 *       aplicación que orquesta la lógica de reclamación, desreclamación y conquista
 *       de territorios.</li>
 *   <li>{@link dev.alexissdev.kronos.claims.repository.ClaimRepository} →
 *       {@link dev.alexissdev.kronos.claims.persistence.MongoClaimRepository} (singleton):
 *       implementación MongoDB del repositorio de claims.</li>
 *   <li>{@link ClaimListener} (singleton): listener de Bukkit y Guava EventBus que
 *       protege los territorios y notifica a los jugadores al cruzar sus límites.</li>
 * </ul>
 *
 * <p>Este módulo debe instalarse en el injector principal del plugin durante la
 * inicialización de Kronos.</p>
 */
public class ClaimsModule extends AbstractModule {

    /**
     * Declara todas las vinculaciones de tipos del módulo de claims.
     *
     * <p>Guice llamará a este método automáticamente al construir el injector. No debe
     * invocarse directamente desde código de aplicación.</p>
     */
    @Override
    protected void configure() {
        bind(ClaimApplicationService.class).in(Singleton.class);
        bind(ClaimService.class).to(ClaimApplicationService.class);

        bind(ClaimRepository.class).to(MongoClaimRepository.class).in(Singleton.class);

        bind(ClaimListener.class).in(Singleton.class);
    }
}
