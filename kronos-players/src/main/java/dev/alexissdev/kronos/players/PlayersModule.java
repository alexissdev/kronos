package dev.alexissdev.kronos.players;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.inventory.CrateInventory;
import dev.alexissdev.kronos.players.persistence.MongoPlayerRepository;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.players.service.PlayerService;

public class PlayersModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PlayerApplicationService.class).in(Singleton.class);
        bind(PlayerService.class).to(PlayerApplicationService.class);
        bind(PlayerRepository.class).to(MongoPlayerRepository.class).in(Singleton.class);
        bind(CrateInventory.class).in(Singleton.class);
    }
}
