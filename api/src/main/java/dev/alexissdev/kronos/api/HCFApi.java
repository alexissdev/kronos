package dev.alexissdev.kronos.api;

import dev.alexissdev.kronos.api.facade.*;

/** Facade principal del HCF plugin para inter-plugin communication via Bukkit ServicesManager. */
public interface HCFApi {

    FactionApi factions();

    PlayerDataApi players();

    TimerApi timers();

    ClaimApi claims();

    KothApi koth();

    String getVersion();
}
