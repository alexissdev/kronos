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

@Singleton
public class KitApplicationService implements KitService {

    private static final long CLASS_COOLDOWN_MS = 10_000L;

    private final PlayerRepository playerRepository;
    private final TimerRepository timerRepository;
    private final TimerApplicationService timerService;

    @Inject
    public KitApplicationService(PlayerRepository playerRepository,
                                 TimerRepository timerRepository,
                                 TimerApplicationService timerService) {
        this.playerRepository = playerRepository;
        this.timerRepository = timerRepository;
        this.timerService = timerService;
    }

    @Override
    public CompletableFuture<Optional<KitType>> detectKit(UUID playerUuid) {
        return playerRepository.findByUuid(playerUuid)
                .thenApply(opt -> opt.map(p -> p.getActiveKit()));
    }

    @Override
    public CompletableFuture<Void> activateClassAbility(UUID playerUuid, KitType kitType) {
        return timerService.startTimer(playerUuid, TimerType.CLASS_COOLDOWN, CLASS_COOLDOWN_MS);
    }

    @Override
    public CompletableFuture<Boolean> isClassAbilityOnCooldown(UUID playerUuid) {
        return timerService.hasActiveTimer(playerUuid, TimerType.CLASS_COOLDOWN);
    }

    @Override
    public CompletableFuture<Void> updateActiveKit(UUID playerUuid, KitType kitType) {
        return playerRepository.findByUuid(playerUuid).thenCompose(opt -> {
            var player = opt.orElseThrow(() -> new PlayerNotFoundException(playerUuid));
            player.setActiveKit(kitType);
            return playerRepository.save(player).thenApply(p -> null);
        });
    }
}
