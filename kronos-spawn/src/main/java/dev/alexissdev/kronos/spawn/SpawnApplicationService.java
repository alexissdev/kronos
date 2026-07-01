package dev.alexissdev.kronos.spawn;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.repository.SpawnRepository;
import dev.alexissdev.kronos.spawn.service.SpawnService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SpawnApplicationService implements SpawnService {

    private volatile SpawnZone cachedZone;

    private final SpawnRepository repository;

    @Inject
    public SpawnApplicationService(SpawnRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<Void> loadZone() {
        return repository.findZone().thenAccept(opt -> cachedZone = opt.orElse(null));
    }

    @Override
    public CompletableFuture<Void> setZone(SpawnZone zone) {
        cachedZone = zone;
        return repository.saveZone(zone);
    }

    @Override
    public CompletableFuture<Void> removeZone() {
        cachedZone = null;
        return repository.deleteZone();
    }

    @Override
    public Optional<SpawnZone> getZone() {
        return Optional.ofNullable(cachedZone);
    }
}
