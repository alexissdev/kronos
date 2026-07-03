package dev.alexissdev.kronos.koth;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.event.KothCapturedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothDeletedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothEndedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothStartedDomainEvent;
import dev.alexissdev.kronos.koth.exception.KothNotFoundException;
import dev.alexissdev.kronos.koth.repository.KothRepository;
import dev.alexissdev.kronos.koth.service.KothService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementación principal del servicio de negocio KOTH para el plugin HCF Kronos.
 *
 * <p>Actúa como la capa de aplicación que coordina el repositorio de persistencia
 * ({@link KothRepository}) y el bus de eventos de Guava ({@link EventBus}). Cada operación
 * de estado importante (iniciar, terminar, capturar, eliminar) actualiza la base de datos
 * MongoDB y publica el evento de dominio correspondiente para que otros módulos puedan
 * reaccionar de forma desacoplada.</p>
 *
 * <p>Es un singleton administrado por Guice y registrado como implementación de
 * {@link KothService} en {@code KothModule}.</p>
 */
@Singleton
public class KothApplicationService implements KothService {

    private final KothRepository kothRepository;
    private final EventBus eventBus;

    /**
     * Constructor inyectado por Guice con las dependencias necesarias.
     *
     * @param kothRepository repositorio de persistencia para zonas KOTH
     * @param eventBus       bus de eventos de Guava para publicar eventos de dominio
     */
    @Inject
    public KothApplicationService(KothRepository kothRepository, EventBus eventBus) {
        this.kothRepository = kothRepository;
        this.eventBus = eventBus;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calcula el centro de la zona de captura para incluirlo en el evento
     * {@link KothStartedDomainEvent}, permitiendo que sistemas externos (p.ej. brújulas)
     * apunten al punto medio de la zona.</p>
     */
    @Override
    public CompletableFuture<Void> startKoth(String name) {
        return kothRepository.findByName(name).thenCompose(opt -> {
            KothZone zone = opt.orElseThrow(() -> new KothNotFoundException(name));
            if (zone.isActive()) {
                throw new IllegalStateException("El KOTH '" + name + "' ya está activo.");
            }
            zone.setActive(true);
            int cx = (zone.getCaptureMinX() + zone.getCaptureMaxX()) / 2;
            int cz = (zone.getCaptureMinZ() + zone.getCaptureMaxZ()) / 2;
            return kothRepository.save(zone).thenRun(() ->
                    eventBus.post(new KothStartedDomainEvent(
                            name, cx, cz, zone.getCaptureTimeSeconds(), zone)));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> endKoth(String name) {
        return kothRepository.findByName(name).thenCompose(opt -> {
            KothZone zone = opt.orElseThrow(() -> new KothNotFoundException(name));
            if (!zone.isActive()) {
                throw new IllegalStateException("El KOTH '" + name + "' no está activo.");
            }
            zone.setActive(false);
            return kothRepository.save(zone).thenRun(() ->
                    eventBus.post(new KothEndedDomainEvent(name)));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<KothZone>> getKoth(String name) {
        return kothRepository.findByName(name);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<List<KothZone>> getActiveKoths() {
        return kothRepository.findAll().thenApply(zones ->
                zones.stream().filter(KothZone::isActive).collect(Collectors.toList()));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<List<KothZone>> getAllKoths() {
        return kothRepository.findAll();
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> captureKoth(String name, UUID captorUuid) {
        return kothRepository.findByName(name).thenCompose(opt -> {
            KothZone zone = opt.orElseThrow(() -> new KothNotFoundException(name));
            zone.setActive(false);
            return kothRepository.save(zone).thenRun(() ->
                    eventBus.post(new KothCapturedDomainEvent(name, captorUuid)));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createKoth(KothZone zone) {
        return kothRepository.save(zone).thenApply(z -> null);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> deleteKoth(String name) {
        return kothRepository.delete(name)
                .thenRun(() -> eventBus.post(new KothDeletedDomainEvent(name)));
    }

    /**
     * Desactiva de forma asíncrona todas las zonas KOTH que estén actualmente activas.
     * Útil al apagar el servidor para garantizar un estado consistente en la base de datos.
     *
     * @return future que se completa cuando todos los KOTHs activos han sido desactivados y persistidos
     */
    public CompletableFuture<Void> deactivateAll() {
        return kothRepository.findAll().thenCompose(zones -> {
            List<CompletableFuture<KothZone>> futures = zones.stream()
                    .filter(KothZone::isActive)
                    .map(zone -> {
                        zone.setActive(false);
                        return kothRepository.save(zone);
                    })
                    .collect(Collectors.toList());
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        });
    }
}
