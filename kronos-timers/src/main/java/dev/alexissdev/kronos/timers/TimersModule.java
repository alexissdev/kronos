package dev.alexissdev.kronos.timers;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.timers.persistence.RedisTimerRepository;
import dev.alexissdev.kronos.timers.repository.TimerRepository;
import dev.alexissdev.kronos.timers.service.TimerService;

public class TimersModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TimerCache.class).in(Singleton.class);
        bind(TimerApplicationService.class).in(Singleton.class);
        bind(TimerService.class).to(TimerApplicationService.class);

        bind(TimerRepository.class).to(RedisTimerRepository.class).in(Singleton.class);
    }
}
