package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.PlayerSnapshot;

import java.util.Optional;
import java.util.UUID;

/** Read-only player stats queries for external plugins. */
public interface PlayerDataApi {

    Optional<PlayerSnapshot> getPlayer(UUID uuid);

    int getKills(UUID uuid);

    int getDeaths(UUID uuid);

    double getBalance(UUID uuid);

    boolean isOnline(UUID uuid);
}
