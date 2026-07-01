package dev.alexissdev.kronos.koth;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.event.KothCapturedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothStartedDomainEvent;
import dev.alexissdev.kronos.koth.exception.KothNotFoundException;
import dev.alexissdev.kronos.koth.repository.KothRepository;
import dev.alexissdev.kronos.koth.service.KothService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class KothApplicationService implements KothService {

    private final KothRepository kothRepository;
    private final EventBus eventBus;

    @Inject
    public KothApplicationService(KothRepository kothRepository, EventBus eventBus) {
        this.kothRepository = kothRepository;
        this.eventBus = eventBus;
    }

    @Override
    public CompletableFuture<Void> startKoth(String name) {
        return kothRepository.findByName(name).thenCompose(opt -> {
            KothZone zone = opt.orElseThrow(() -> new KothNotFoundException(name));
            zone.setActive(true);
            return kothRepository.save(zone).thenRun(() ->
                    eventBus.post(new KothStartedDomainEvent(name)));
        });
    }

    @Override
    public CompletableFuture<Void> endKoth(String name) {
        return kothRepository.findByName(name).thenCompose(opt -> {
            KothZone zone = opt.orElseThrow(() -> new KothNotFoundException(name));
            zone.setActive(false);
            return kothRepository.save(zone).thenApply(z -> null);
        });
    }

    @Override
    public CompletableFuture<Optional<KothZone>> getKoth(String name) {
        return kothRepository.findByName(name);
    }

    @Override
    public CompletableFuture<List<KothZone>> getActiveKoths() {
        return kothRepository.findAll().thenApply(zones ->
                zones.stream().filter(KothZone::isActive).collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<KothZone>> getAllKoths() {
        return kothRepository.findAll();
    }

    @Override
    public CompletableFuture<Void> captureKoth(String name, UUID captorUuid) {
        return kothRepository.findByName(name).thenCompose(opt -> {
            KothZone zone = opt.orElseThrow(() -> new KothNotFoundException(name));
            zone.setActive(false);
            return kothRepository.save(zone).thenRun(() ->
                    eventBus.post(new KothCapturedDomainEvent(name, captorUuid)));
        });
    }

    @Override
    public CompletableFuture<Void> createKoth(KothZone zone) {
        return kothRepository.save(zone).thenApply(z -> null);
    }

    @Override
    public CompletableFuture<Void> deleteKoth(String name) {
        return kothRepository.delete(name);
    }
}
