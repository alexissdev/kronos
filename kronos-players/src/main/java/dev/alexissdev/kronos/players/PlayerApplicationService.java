package dev.alexissdev.kronos.players;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.domain.HCFPlayer;
import dev.alexissdev.kronos.players.exception.PlayerNotFoundException;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.players.service.PlayerService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PlayerApplicationService implements PlayerService {

    private final PlayerRepository playerRepository;

    @Inject
    public PlayerApplicationService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public CompletableFuture<HCFPlayer> getOrCreate(UUID uuid, String name) {
        return playerRepository.findByUuid(uuid).thenCompose(opt -> {
            if (opt.isPresent()) {
                HCFPlayer player = opt.get();
                if (!player.getName().equals(name)) {
                    player.setName(name);
                    return playerRepository.save(player);
                }
                return CompletableFuture.completedFuture(player);
            }
            HCFPlayer newPlayer = new HCFPlayer(uuid, name);
            return playerRepository.save(newPlayer);
        });
    }

    @Override
    public CompletableFuture<Optional<HCFPlayer>> getPlayer(UUID uuid) {
        return playerRepository.findByUuid(uuid);
    }

    @Override
    public CompletableFuture<Void> savePlayer(HCFPlayer player) {
        return playerRepository.save(player).thenApply(p -> null);
    }

    @Override
    public CompletableFuture<Void> recordKill(UUID killerUuid, UUID victimUuid) {
        CompletableFuture<Optional<HCFPlayer>> killerFuture = playerRepository.findByUuid(killerUuid);
        CompletableFuture<Optional<HCFPlayer>> victimFuture = playerRepository.findByUuid(victimUuid);

        return killerFuture.thenCombine(victimFuture, (killerOpt, victimOpt) -> {
            HCFPlayer killer = killerOpt.orElseThrow(() -> new PlayerNotFoundException(killerUuid));
            HCFPlayer victim = victimOpt.orElseThrow(() -> new PlayerNotFoundException(victimUuid));
            killer.incrementKills();
            victim.incrementDeaths();
            return playerRepository.save(killer)
                    .thenCompose(k -> playerRepository.save(victim));
        }).thenCompose(f -> f).thenApply(p -> null);
    }
}
