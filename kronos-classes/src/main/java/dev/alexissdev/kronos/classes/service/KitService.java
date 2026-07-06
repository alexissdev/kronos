package dev.alexissdev.kronos.classes.service;

import dev.alexissdev.kronos.players.domain.KitType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Application port defining the operations available over player classes (kits).
 *
 * <p>Abstracts the logic for detecting the active kit, managing ability cooldowns, and
 * persisting the selected kit. The primary implementation is
 * {@link dev.alexissdev.kronos.classes.KitApplicationService}, injected by Guice.
 * All operations are asynchronous.</p>
 */
public interface KitService {

    /**
     * Retrieves the player's currently active kit by querying their persisted profile.
     *
     * <p>Called when the player joins the server to initialise the local cache of
     * {@link dev.alexissdev.kronos.classes.listener.ClassListener}.</p>
     *
     * @param playerUuid UUID of the player
     * @return future with the active {@link KitType}, or empty if the player has no kit assigned
     */
    CompletableFuture<Optional<KitType>> detectKit(UUID playerUuid);

    /**
     * Records the activation of a class's active ability and starts its cooldown timer.
     *
     * <p>Starts the {@code CLASS_COOLDOWN} timer for the player, preventing the ability
     * from being used again until the cooldown expires.</p>
     *
     * @param playerUuid UUID of the player activating the ability
     * @param kitType    the class whose ability was activated
     * @return future that completes when the cooldown has been registered
     */
    CompletableFuture<Void> activateClassAbility(UUID playerUuid, KitType kitType);

    /**
     * Checks whether the player's active ability is currently on cooldown.
     *
     * @param playerUuid UUID of the player to query
     * @return future with {@code true} if the cooldown is active; {@code false} otherwise
     */
    CompletableFuture<Boolean> isClassAbilityOnCooldown(UUID playerUuid);

    /**
     * Updates and persists the player's active kit.
     *
     * <p>Called when the player changes their helmet to synchronise the class change
     * detected in real time with their stored profile in the database.</p>
     *
     * @param playerUuid UUID of the player
     * @param kitType    the new kit the player has equipped
     * @return future that completes when the profile has been updated
     * @throws dev.alexissdev.kronos.players.exception.PlayerNotFoundException if the player has no registered profile
     */
    CompletableFuture<Void> updateActiveKit(UUID playerUuid, KitType kitType);
}
