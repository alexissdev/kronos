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
 * Implementación principal del servicio de aplicación para las clases (kits) de jugadores HCF.
 *
 * <p>Coordina la lógica de negocio relacionada con los kits: detectar el kit activo de un
 * jugador, activar y verificar el cooldown de habilidades, y persistir los cambios de kit.
 * Delega en {@link PlayerRepository} para leer y actualizar perfiles de jugador, y en
 * {@link TimerApplicationService} para gestionar el temporizador de cooldown de habilidades.</p>
 *
 * <p>El cooldown de habilidad está fijado en {@value #CLASS_COOLDOWN_MS} ms (10 segundos)
 * y se gestiona mediante el timer de tipo {@link dev.alexissdev.kronos.timers.domain.TimerType#CLASS_COOLDOWN}.</p>
 *
 * <p>Registrada como singleton por Guice a través de {@link ClassesModule}.</p>
 */
@Singleton
public class KitApplicationService implements KitService {

    /** Duración en milisegundos del cooldown aplicado tras activar una habilidad de clase. */
    private static final long CLASS_COOLDOWN_MS = 10_000L;

    private final PlayerRepository playerRepository;
    private final TimerRepository timerRepository;
    private final TimerApplicationService timerService;

    /**
     * Construye el servicio inyectando sus dependencias.
     *
     * @param playerRepository repositorio de perfiles de jugador para leer y actualizar kits
     * @param timerRepository  repositorio de temporizadores (inyectado para uso futuro o extensión)
     * @param timerService     servicio de timers para iniciar y consultar el cooldown de habilidades
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
     * <p>Consulta el repositorio de jugadores y extrae el kit activo del perfil almacenado.</p>
     */
    @Override
    public CompletableFuture<Optional<KitType>> detectKit(UUID playerUuid) {
        return playerRepository.findByUuid(playerUuid)
                .thenApply(opt -> opt.map(p -> p.getActiveKit()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Inicia el timer {@code CLASS_COOLDOWN} con una duración de {@value #CLASS_COOLDOWN_MS} ms.
     * El parámetro {@code kitType} está disponible para extensiones futuras (por ejemplo,
     * cooldowns distintos por clase), pero actualmente no varía la duración.</p>
     */
    @Override
    public CompletableFuture<Void> activateClassAbility(UUID playerUuid, KitType kitType) {
        return timerService.startTimer(playerUuid, TimerType.CLASS_COOLDOWN, CLASS_COOLDOWN_MS);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Consulta el servicio de timers para verificar si el timer {@code CLASS_COOLDOWN}
     * sigue activo para el jugador indicado.</p>
     */
    @Override
    public CompletableFuture<Boolean> isClassAbilityOnCooldown(UUID playerUuid) {
        return timerService.hasActiveTimer(playerUuid, TimerType.CLASS_COOLDOWN);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Recupera el perfil del jugador, actualiza su kit activo en memoria y lo guarda
     * en el repositorio. Si el jugador no existe, lanza
     * {@link dev.alexissdev.kronos.players.exception.PlayerNotFoundException}.</p>
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
