package dev.alexissdev.kronos.players;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.players.domain.CrateLocation;
import dev.alexissdev.kronos.players.repository.CrateLocationRepository;
import dev.alexissdev.kronos.players.service.CrateService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CrateApplicationService implements CrateService {

    private final CrateLocationRepository repository;

    @Inject
    public CrateApplicationService(CrateLocationRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompletableFuture<CrateLocation> setCrate(String world, int x, int y, int z, CrateType type) {
        return repository.findByLocation(world, x, y, z).thenCompose(existing -> {
            String id = existing.map(CrateLocation::getId).orElse(UUID.randomUUID().toString());
            return repository.save(new CrateLocation(id, world, x, y, z, type));
        });
    }

    @Override
    public CompletableFuture<Void> removeCrate(String world, int x, int y, int z) {
        return repository.findByLocation(world, x, y, z).thenCompose(opt -> {
            CrateLocation loc = opt.orElseThrow(() -> new HCFException("No hay ningún crate en esa ubicación"));
            return repository.delete(loc.getId());
        });
    }

    @Override
    public CompletableFuture<Optional<CrateLocation>> getCrateAt(String world, int x, int y, int z) {
        return repository.findByLocation(world, x, y, z);
    }

    @Override
    public CompletableFuture<List<CrateLocation>> getAllCrates() {
        return repository.findAll();
    }
}
