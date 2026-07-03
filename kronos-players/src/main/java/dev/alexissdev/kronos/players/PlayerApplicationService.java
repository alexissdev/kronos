package dev.alexissdev.kronos.players;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dev.alexissdev.kronos.players.domain.HCFPlayer;
import dev.alexissdev.kronos.players.exception.PlayerNotFoundException;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.players.service.PlayerService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación principal para la gestión de perfiles de jugadores HCF.
 *
 * <p>Implementa {@link PlayerService} coordinando la persistencia de datos del jugador
 * en MongoDB ({@link PlayerRepository}) y el estado del Deathban en Redis
 * ({@link DeathbanRepository}). Orquesta la lógica de negocio del ciclo de vida completo
 * del jugador: creación del perfil al primer ingreso, actualización de nombre, gestión de
 * vidas, registro de estadísticas de combate y control del sistema de Deathban.</p>
 *
 * <p>Esta clase es un singleton gestionado por Guice y sus dependencias son inyectadas
 * automáticamente mediante el módulo {@link PlayersModule}.</p>
 */
@Singleton
public class PlayerApplicationService implements PlayerService {

    private final PlayerRepository playerRepository;
    private final DeathbanRepository deathbanRepository;
    private final int defaultLives;

    /**
     * Crea el servicio de aplicación con sus dependencias inyectadas por Guice.
     *
     * @param playerRepository   repositorio para leer y escribir perfiles de jugadores en MongoDB
     * @param deathbanRepository repositorio para gestionar el estado de Deathban en Redis
     * @param defaultLives       número de vidas con el que se registra un jugador nuevo o se restaura
     *                           tras un Deathban expirado; configurado con la clave {@code hcf.lives}
     */
    @Inject
    public PlayerApplicationService(PlayerRepository playerRepository,
                                    DeathbanRepository deathbanRepository,
                                    @Named("hcf.lives") int defaultLives) {
        this.playerRepository    = playerRepository;
        this.deathbanRepository  = deathbanRepository;
        this.defaultLives        = defaultLives;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Si el jugador ya existe en la base de datos, actualiza su nombre si cambió
     * y restaura sus vidas al valor predeterminado si están en cero (indica que el
     * Deathban expiró y el jugador puede volver a jugar). Si el jugador es nuevo,
     * se crea un perfil con los valores por defecto del servidor.</p>
     */
    @Override
    public CompletableFuture<HCFPlayer> getOrCreate(UUID uuid, String name) {
        return playerRepository.findByUuid(uuid).thenCompose(opt -> {
            if (opt.isPresent()) {
                HCFPlayer player = opt.get();
                boolean needsSave = false;
                if (!player.getName().equals(name)) {
                    player.setName(name);
                    needsSave = true;
                }
                // Reconnected after deathban expired — restore lives
                if (player.getLives() <= 0) {
                    player.setLives(defaultLives);
                    needsSave = true;
                }
                if (needsSave) {
                    return playerRepository.save(player);
                }
                return CompletableFuture.completedFuture(player);
            }
            HCFPlayer newPlayer = new HCFPlayer(uuid, name);
            return playerRepository.save(newPlayer);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Optional<HCFPlayer>> getPlayer(UUID uuid) {
        return playerRepository.findByUuid(uuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> savePlayer(HCFPlayer player) {
        return playerRepository.save(player).thenApply(p -> null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Recupera ambos perfiles en paralelo con {@link CompletableFuture#thenCombine} para
     * minimizar la latencia, luego incrementa las estadísticas y persiste ambos perfiles.</p>
     *
     * @throws PlayerNotFoundException si alguno de los dos jugadores no existe en la base de datos
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Recupera el perfil del jugador, decrementa sus vidas usando
     * {@link HCFPlayer#decrementLives()} y persiste el cambio en MongoDB.
     * Si el jugador no existe en la base de datos, devuelve {@code 0} sin lanzar excepción.</p>
     */
    @Override
    public CompletableFuture<Integer> decrementLives(UUID uuid) {
        return playerRepository.findByUuid(uuid).thenCompose(opt -> {
            if (!opt.isPresent()) return CompletableFuture.completedFuture(0);
            HCFPlayer player = opt.get();
            int remaining = player.decrementLives();
            return playerRepository.save(player).thenApply(p -> remaining);
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Consulta el TTL del Deathban en Redis. Si el TTL es mayor que cero,
     * el jugador sigue baneado y no puede ingresar al servidor.</p>
     */
    @Override
    public CompletableFuture<Boolean> isDeathbanned(UUID uuid) {
        return deathbanRepository.getRemainingSeconds(uuid)
                .thenApply(opt -> opt.isPresent() && opt.getAsLong() > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> removeDeathban(UUID uuid) {
        return deathbanRepository.removeDeathban(uuid);
    }
}
