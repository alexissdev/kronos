package dev.alexissdev.kronos.classes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.players.exception.PlayerNotFoundException;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.timers.repository.TimerRepository;
import dev.alexissdev.kronos.classes.service.KitService;
import dev.alexissdev.kronos.timers.TimerApplicationService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Primary application service implementation for HCF player class (kit) management.
 *
 * <p>Coordinates the business logic related to kits: detecting a player's active kit,
 * activating and verifying ability cooldowns, and persisting kit changes. Delegates to
 * {@link PlayerRepository} for reading and updating player profiles, and to
 * {@link TimerApplicationService} for managing the ability cooldown timer.</p>
 *
 * <p>The ability cooldown is fixed at {@value #CLASS_COOLDOWN_MS} ms (10 seconds) and
 * is managed through the timer of type
 * {@link dev.alexissdev.kronos.timers.domain.TimerType#CLASS_COOLDOWN}.</p>
 *
 * <p>Registered as a singleton by Guice through {@link ClassesModule}.</p>
 */
@Singleton
public class KitApplicationService implements KitService {

    /** Duration in milliseconds of the cooldown applied after activating a class ability. */
    private static final long CLASS_COOLDOWN_MS = 10_000L;

    private final PlayerRepository playerRepository;
    private final TimerRepository timerRepository;
    private final TimerApplicationService timerService;

    /**
     * Constructs the service by injecting its dependencies.
     *
     * @param playerRepository repository of player profiles for reading and updating kits
     * @param timerRepository  timer repository (injected for future use or extension)
     * @param timerService     timer service for starting and querying the ability cooldown
     */
    @Inject
    public KitApplicationService(PlayerRepository playerRepository,
                                 TimerRepository timerRepository,
                                 TimerApplicationService timerService) {
        this.playerRepository = playerRepository;
        this.timerRepository = timerRepository;
        this.timerService = timerService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries the player repository and extracts the active kit from the stored profile.</p>
     */
    @Override
    public CompletableFuture<Optional<KitType>> detectKit(UUID playerUuid) {
        return playerRepository.findByUuid(playerUuid)
                .thenApply(opt -> opt.map(p -> p.getActiveKit()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Starts the {@code CLASS_COOLDOWN} timer with a duration of {@value #CLASS_COOLDOWN_MS} ms.
     * The {@code kitType} parameter is available for future extensions (e.g., per-class cooldown
     * durations) but does not affect the duration at this time.</p>
     */
    @Override
    public CompletableFuture<Void> activateClassAbility(UUID playerUuid, KitType kitType) {
        return timerService.startTimer(playerUuid, TimerType.CLASS_COOLDOWN, CLASS_COOLDOWN_MS);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries the timer service to check whether the {@code CLASS_COOLDOWN} timer is
     * still active for the given player.</p>
     */
    @Override
    public CompletableFuture<Boolean> isClassAbilityOnCooldown(UUID playerUuid) {
        return timerService.hasActiveTimer(playerUuid, TimerType.CLASS_COOLDOWN);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the player's profile, updates their active kit in memory, and saves it
     * back to the repository. Throws
     * {@link dev.alexissdev.kronos.players.exception.PlayerNotFoundException} if the
     * player has no registered profile.</p>
     */
    @Override
    public CompletableFuture<Void> updateActiveKit(UUID playerUuid, KitType kitType) {
        return playerRepository.findByUuid(playerUuid).thenCompose(opt -> {
            var player = opt.orElseThrow(() -> new PlayerNotFoundException(playerUuid));
            player.setActiveKit(kitType);
            return playerRepository.save(player).thenApply(p -> null);
        });
    }
}
