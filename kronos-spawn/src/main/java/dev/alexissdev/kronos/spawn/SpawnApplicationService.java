package dev.alexissdev.kronos.spawn;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.spawn.domain.SpawnZone;
import dev.alexissdev.kronos.spawn.repository.SpawnRepository;
import dev.alexissdev.kronos.spawn.service.SpawnService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación principal del servicio de negocio para la zona de spawn del servidor HCF.
 *
 * <p>Mantiene un caché en memoria ({@code cachedZone}) de la zona activa para permitir
 * consultas síncronas y eficientes desde el hilo principal de Bukkit (p.ej. en
 * {@link dev.alexissdev.kronos.spawn.listener.SpawnListener} durante eventos de movimiento).
 * La variable es declarada {@code volatile} para garantizar visibilidad entre hilos.</p>
 *
 * <p>Las operaciones de escritura ({@link #setZone(SpawnZone)}, {@link #removeZone()})
 * actualizan el caché de inmediato y persisten en MongoDB de forma asíncrona.</p>
 *
 * <p>Es un singleton administrado por Guice, registrado en {@code SpawnModule}.</p>
 */
@Singleton
public class SpawnApplicationService implements SpawnService {

    private volatile SpawnZone cachedZone;

    private final SpawnRepository repository;

    /**
     * Constructor inyectado por Guice.
     *
     * @param repository repositorio de persistencia para la zona de spawn
     */
    @Inject
    public SpawnApplicationService(SpawnRepository repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Carga el documento de spawn desde MongoDB y almacena el resultado en {@code cachedZone}.
     * Debe llamarse al arrancar el plugin.</p>
     */
    @Override
    public CompletableFuture<Void> loadZone() {
        return repository.findZone().thenAccept(opt -> cachedZone = opt.orElse(null));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Actualiza el caché en memoria de forma inmediata antes de la escritura asíncrona,
     * para que los listeners detecten la nueva zona sin esperar la respuesta de MongoDB.</p>
     */
    @Override
    public CompletableFuture<Void> setZone(SpawnZone zone) {
        cachedZone = zone;
        return repository.saveZone(zone);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> removeZone() {
        cachedZone = null;
        return repository.deleteZone();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<SpawnZone> getZone() {
        return Optional.ofNullable(cachedZone);
    }
}
