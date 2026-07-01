package dev.alexissdev.kronos.factions;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.factions.inventory.FactionInventory;
import dev.alexissdev.kronos.factions.persistence.MongoFactionRepository;
import dev.alexissdev.kronos.factions.repository.FactionRepository;
import dev.alexissdev.kronos.factions.service.FactionService;

public class FactionsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FactionApplicationService.class).in(Singleton.class);
        bind(FactionService.class).to(FactionApplicationService.class);

        bind(FactionRepository.class).to(MongoFactionRepository.class).in(Singleton.class);

        bind(FactionInventory.class).in(Singleton.class);
    }
}
