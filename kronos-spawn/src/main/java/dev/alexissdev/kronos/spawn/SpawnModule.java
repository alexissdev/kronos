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

public class SpawnModule extends AbstractModule {

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
