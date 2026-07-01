package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.TimerSnapshot;
import dev.alexissdev.kronos.core.domain.TimerType;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/** Timer state queries for external plugins. */
public interface TimerApi {

    boolean hasCombatTag(UUID uuid);

    boolean hasPvpTimer(UUID uuid);

    boolean hasEnderpearlCooldown(UUID uuid);

    OptionalLong getRemainingMillis(UUID uuid, TimerType type);

    Optional<TimerSnapshot> getTimer(UUID uuid, TimerType type);
}
