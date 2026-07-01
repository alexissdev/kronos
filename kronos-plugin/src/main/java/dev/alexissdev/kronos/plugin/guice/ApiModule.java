package dev.alexissdev.kronos.plugin.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.HCFApi;
import dev.alexissdev.kronos.api.HCFApiImpl;

public class ApiModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HCFApi.class).to(HCFApiImpl.class).in(Singleton.class);
        bind(HCFApiImpl.class).in(Singleton.class);
    }
}
