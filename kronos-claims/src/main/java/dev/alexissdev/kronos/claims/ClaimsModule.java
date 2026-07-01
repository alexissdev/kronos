package dev.alexissdev.kronos.claims;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.listener.ClaimListener;
import dev.alexissdev.kronos.claims.persistence.MongoClaimRepository;
import dev.alexissdev.kronos.claims.repository.ClaimRepository;
import dev.alexissdev.kronos.claims.service.ClaimService;

public class ClaimsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ClaimApplicationService.class).in(Singleton.class);
        bind(ClaimService.class).to(ClaimApplicationService.class);

        bind(ClaimRepository.class).to(MongoClaimRepository.class).in(Singleton.class);

        bind(ClaimListener.class).in(Singleton.class);
    }
}
