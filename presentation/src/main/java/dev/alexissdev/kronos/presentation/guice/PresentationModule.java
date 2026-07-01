package dev.alexissdev.kronos.presentation.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.presentation.command.*;
import dev.alexissdev.kronos.presentation.inventory.CrateInventory;
import dev.alexissdev.kronos.presentation.inventory.FactionInventory;
import dev.alexissdev.kronos.presentation.listener.*;

public class PresentationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FactionCommand.class).in(Singleton.class);
        bind(KothCommand.class).in(Singleton.class);
        bind(MoneyCommand.class).in(Singleton.class);
        bind(StaffCommand.class).in(Singleton.class);
        bind(HCFCommand.class).in(Singleton.class);

        bind(PlayerDataListener.class).in(Singleton.class);
        bind(PvpListener.class).in(Singleton.class);
        bind(ClaimListener.class).in(Singleton.class);
        bind(ClassListener.class).in(Singleton.class);
        bind(TimerListener.class).in(Singleton.class);
        bind(KothListener.class).in(Singleton.class);
        bind(FactionEventListener.class).in(Singleton.class);

        bind(CrateInventory.class).in(Singleton.class);
        bind(FactionInventory.class).in(Singleton.class);
    }
}
