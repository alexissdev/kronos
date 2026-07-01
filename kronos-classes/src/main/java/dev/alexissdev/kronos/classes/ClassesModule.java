package dev.alexissdev.kronos.classes;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.classes.listener.ClassListener;
import dev.alexissdev.kronos.classes.service.KitService;

public class ClassesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(KitApplicationService.class).in(Singleton.class);
        bind(KitService.class).to(KitApplicationService.class);

        bind(ClassListener.class).in(Singleton.class);
    }
}
