package dev.alexissdev.kronos.koth;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.koth.command.KothCommand;
import dev.alexissdev.kronos.koth.creation.KothCreationService;
import dev.alexissdev.kronos.koth.listener.KothWandListener;
import dev.alexissdev.kronos.koth.persistence.MongoKothRepository;
import dev.alexissdev.kronos.koth.repository.KothRepository;
import dev.alexissdev.kronos.koth.service.KothService;

public class KothModule extends AbstractModule {

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
