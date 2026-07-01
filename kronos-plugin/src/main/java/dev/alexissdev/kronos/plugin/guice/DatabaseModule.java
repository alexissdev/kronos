package dev.alexissdev.kronos.plugin.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.common.database.RedisConnectionFactory;

public class DatabaseModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MongoConnectionFactory.class).in(Singleton.class);
        bind(RedisConnectionFactory.class).in(Singleton.class);
    }
}
