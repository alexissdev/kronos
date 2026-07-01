package dev.alexissdev.kronos.players;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.domain.HCFPlayer;
import dev.alexissdev.kronos.players.exception.PlayerNotFoundException;
import dev.alexissdev.kronos.players.repository.FreezeRepository;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.players.service.StaffService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class StaffApplicationService implements StaffService {

    private final PlayerRepository playerRepository;
    private final FreezeRepository freezeRepository;

    @Inject
    public StaffApplicationService(PlayerRepository playerRepository,
                                   FreezeRepository freezeRepository) {
        this.playerRepository = playerRepository;
        this.freezeRepository = freezeRepository;
    }

    @Override
    public CompletableFuture<Void> enableStaffMode(UUID staffUuid) {
        return playerRepository.findByUuid(staffUuid).thenCompose(opt -> {
            HCFPlayer player = opt.orElseThrow(() -> new PlayerNotFoundException(staffUuid));
            player.setStaffMode(true);
            return playerRepository.save(player).thenApply(p -> null);
        });
    }

    @Override
    public CompletableFuture<Void> disableStaffMode(UUID staffUuid) {
        return playerRepository.findByUuid(staffUuid).thenCompose(opt -> {
            HCFPlayer player = opt.orElseThrow(() -> new PlayerNotFoundException(staffUuid));
            player.setStaffMode(false);
            player.setSavedInventoryJson(null);
            return playerRepository.save(player).thenApply(p -> null);
        });
    }

    @Override
    public CompletableFuture<Void> setVanish(UUID staffUuid, boolean vanished) {
        return playerRepository.findByUuid(staffUuid).thenCompose(opt -> {
            HCFPlayer player = opt.orElseThrow(() -> new PlayerNotFoundException(staffUuid));
            player.setVanished(vanished);
            return playerRepository.save(player).thenApply(p -> null);
        });
    }

    @Override
    public CompletableFuture<Void> freeze(UUID staffUuid, UUID targetUuid) {
        return freezeRepository.freeze(staffUuid, targetUuid);
    }

    @Override
    public CompletableFuture<Void> unfreeze(UUID targetUuid) {
        return freezeRepository.unfreeze(targetUuid);
    }

    @Override
    public CompletableFuture<Boolean> isFrozen(UUID playerUuid) {
        return freezeRepository.isFrozen(playerUuid);
    }

    @Override
    public CompletableFuture<Boolean> isInStaffMode(UUID staffUuid) {
        return playerRepository.findByUuid(staffUuid)
                .thenApply(opt -> opt.map(HCFPlayer::isStaffMode).orElse(false));
    }

    @Override
    public CompletableFuture<Boolean> isVanished(UUID playerUuid) {
        return playerRepository.findByUuid(playerUuid)
                .thenApply(opt -> opt.map(HCFPlayer::isVanished).orElse(false));
    }
}
