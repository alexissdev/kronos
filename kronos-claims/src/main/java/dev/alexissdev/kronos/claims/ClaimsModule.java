package dev.alexissdev.kronos.claims;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.listener.ClaimListener;
import dev.alexissdev.kronos.claims.persistence.MongoClaimRepository;
import dev.alexissdev.kronos.claims.repository.ClaimRepository;
import dev.alexissdev.kronos.claims.service.ClaimService;

/**
 * Guice module that configures the dependency bindings for the territory (claims) subsystem.
 *
 * <p>Registers the following bindings in the dependency-injection container:</p>
 * <ul>
 *   <li>{@link ClaimService} → {@link ClaimApplicationService} (singleton): application
 *       service that orchestrates the logic for claiming, unclaiming, and overclaiming
 *       territories.</li>
 *   <li>{@link dev.alexissdev.kronos.claims.repository.ClaimRepository} →
 *       {@link dev.alexissdev.kronos.claims.persistence.MongoClaimRepository} (singleton):
 *       MongoDB-backed implementation of the claim repository.</li>
 *   <li>{@link ClaimListener} (singleton): Bukkit and Guava EventBus listener that
 *       protects territories and notifies players when they cross claim boundaries.</li>
 * </ul>
 *
 * <p>This module must be installed in the plugin's main injector during Kronos
 * initialisation.</p>
 */
public class ClaimsModule extends AbstractModule {

    /**
     * Declares all type bindings for the claims module.
     *
     * <p>Guice will call this method automatically when building the injector. It should
     * never be invoked directly from application code.</p>
     */
    @Override
    protected void configure() {
        bind(ClaimApplicationService.class).in(Singleton.class);
        bind(ClaimService.class).to(ClaimApplicationService.class);

        bind(ClaimRepository.class).to(MongoClaimRepository.class).in(Singleton.class);

        bind(ClaimListener.class).in(Singleton.class);
    }
}
