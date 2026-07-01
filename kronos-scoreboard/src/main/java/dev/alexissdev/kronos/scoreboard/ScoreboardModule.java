package dev.alexissdev.kronos.scoreboard;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class ScoreboardModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ScoreboardRenderer.class).in(Singleton.class);
        bind(ScoreboardManager.class).in(Singleton.class);
        bind(ScoreboardListener.class).in(Singleton.class);
        bind(ScoreboardTask.class).in(Singleton.class);
    }
}
