package dev.alexissdev.kronos.players;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.command.StaffCommand;
import dev.alexissdev.kronos.players.inventory.CrateInventory;
import dev.alexissdev.kronos.players.persistence.MongoPlayerRepository;
import dev.alexissdev.kronos.players.persistence.RedisFreezeRepository;
import dev.alexissdev.kronos.players.repository.FreezeRepository;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.players.service.StaffService;

public class PlayersModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PlayerApplicationService.class).in(Singleton.class);
        bind(PlayerService.class).to(PlayerApplicationService.class);
        bind(StaffApplicationService.class).in(Singleton.class);
        bind(StaffService.class).to(StaffApplicationService.class);

        bind(PlayerRepository.class).to(MongoPlayerRepository.class).in(Singleton.class);
        bind(FreezeRepository.class).to(RedisFreezeRepository.class).in(Singleton.class);

        bind(StaffCommand.class).in(Singleton.class);
        bind(CrateInventory.class).in(Singleton.class);
    }
}
