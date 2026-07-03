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

/**
 * Servicio de aplicación para la gestión de crates (cofres de recompensas) en el mapa.
 *
 * <p>Implementa {@link CrateService} delegando la persistencia en {@link CrateLocationRepository}.
 * Se encarga de registrar nuevas ubicaciones de crates, actualizar crates existentes en las
 * mismas coordenadas, eliminarlos y consultarlos. Los crates permiten a los jugadores obtener
 * recompensas aleatorias al interactuar con cofres ubicados en posiciones específicas del mundo.</p>
 *
 * <p>Esta clase es un singleton gestionado por Guice y forma parte del módulo {@link PlayersModule}.</p>
 */
@Singleton
public class CrateApplicationService implements CrateService {

    private final CrateLocationRepository repository;

    /**
     * Crea el servicio con el repositorio de ubicaciones de crates inyectado por Guice.
     *
     * @param repository repositorio de persistencia de ubicaciones de crates
     */
    @Inject
    public CrateApplicationService(CrateLocationRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verifica si ya existe un crate en las coordenadas dadas para reutilizar su ID en
     * la actualización. Si no existe, genera un nuevo UUID como identificador.</p>
     */
    @Override
    public CompletableFuture<CrateLocation> setCrate(String world, int x, int y, int z, CrateType type) {
        return repository.findByLocation(world, x, y, z).thenCompose(existing -> {
            String id = existing.map(CrateLocation::getId).orElse(UUID.randomUUID().toString());
            return repository.save(new CrateLocation(id, world, x, y, z, type));
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws HCFException si no existe ningún crate registrado en las coordenadas indicadas
     */
    @Override
    public CompletableFuture<Void> removeCrate(String world, int x, int y, int z) {
        return repository.findByLocation(world, x, y, z).thenCompose(opt -> {
            CrateLocation loc = opt.orElseThrow(() -> new HCFException("No hay ningún crate en esa ubicación"));
            return repository.delete(loc.getId());
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Optional<CrateLocation>> getCrateAt(String world, int x, int y, int z) {
        return repository.findByLocation(world, x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<List<CrateLocation>> getAllCrates() {
        return repository.findAll();
    }
}
