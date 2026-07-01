package dev.alexissdev.kronos.application.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.application.claim.ClaimApplicationService;
import dev.alexissdev.kronos.application.faction.FactionApplicationService;
import dev.alexissdev.kronos.application.kit.KitApplicationService;
import dev.alexissdev.kronos.application.koth.KothApplicationService;
import dev.alexissdev.kronos.application.player.PlayerApplicationService;
import dev.alexissdev.kronos.application.staff.StaffApplicationService;
import dev.alexissdev.kronos.application.timer.TimerApplicationService;
import dev.alexissdev.kronos.application.timer.TimerCache;
import dev.alexissdev.kronos.core.service.*;

@SuppressWarnings("rawtypes")
public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EventBus.class).in(Singleton.class);
        bind(TimerCache.class).in(Singleton.class);

        bind(FactionApplicationService.class).in(Singleton.class);
        bind(FactionService.class).to(FactionApplicationService.class);

        bind(TimerApplicationService.class).in(Singleton.class);
        bind(TimerService.class).to(TimerApplicationService.class);

        bind(PlayerApplicationService.class).in(Singleton.class);
        bind(PlayerService.class).to(PlayerApplicationService.class);

        bind(ClaimApplicationService.class).in(Singleton.class);
        bind(ClaimService.class).to(ClaimApplicationService.class);

        bind(KothApplicationService.class).in(Singleton.class);
        bind(KotHService.class).to(KothApplicationService.class);

        bind(KitApplicationService.class).in(Singleton.class);
        bind(KitService.class).to(KitApplicationService.class);

        bind(StaffApplicationService.class).in(Singleton.class);
        bind(StaffService.class).to(StaffApplicationService.class);
    }
}
