package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.FactionSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only faction queries for external plugins. */
public interface FactionApi {

    Optional<FactionSnapshot> getByPlayer(UUID playerUuid);

    Optional<FactionSnapshot> getById(String factionId);

    Optional<FactionSnapshot> getByName(String name);

    List<FactionSnapshot> getTopFactions(int limit);

    boolean isInFaction(UUID playerUuid);

    boolean areAllies(String factionIdA, String factionIdB);

    boolean areEnemies(String factionIdA, String factionIdB);
}
