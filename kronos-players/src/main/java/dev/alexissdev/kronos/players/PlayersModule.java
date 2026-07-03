package dev.alexissdev.kronos.players;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.inventory.CrateInventory;
import dev.alexissdev.kronos.players.persistence.MongoCrateLocationRepository;
import dev.alexissdev.kronos.players.persistence.MongoPlayerRepository;
import dev.alexissdev.kronos.players.persistence.RedisDeathbanRepository;
import dev.alexissdev.kronos.players.repository.CrateLocationRepository;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import dev.alexissdev.kronos.players.repository.PlayerRepository;
import dev.alexissdev.kronos.players.service.CrateService;
import dev.alexissdev.kronos.players.service.KitService;
import dev.alexissdev.kronos.players.service.PlayerService;

/**
 * Módulo Guice del subsistema de jugadores del plugin HCF.
 *
 * <p>Configura todas las vinculaciones de inyección de dependencias necesarias para
 * el módulo {@code kronos-players}: interfaces de servicio a sus implementaciones,
 * interfaces de repositorio a sus implementaciones de persistencia, y la clase de
 * animación de inventario de crates.</p>
 *
 * <p>Vinculaciones registradas:</p>
 * <ul>
 *   <li>{@link PlayerService} → {@link PlayerApplicationService} (MongoDB + Redis)</li>
 *   <li>{@link CrateService} → {@link CrateApplicationService} (MongoDB)</li>
 *   <li>{@link KitService} → {@link KitApplicationService}</li>
 *   <li>{@link PlayerRepository} → {@link MongoPlayerRepository}</li>
 *   <li>{@link DeathbanRepository} → {@link RedisDeathbanRepository}</li>
 *   <li>{@link CrateLocationRepository} → {@link MongoCrateLocationRepository}</li>
 *   <li>{@link CrateInventory} como singleton de UI de inventario</li>
 * </ul>
 */
public class PlayersModule extends AbstractModule {

    /**
     * Configura las vinculaciones de dependencias del módulo de jugadores.
     * Todos los servicios y repositorios se registran como singletons para garantizar
     * una única instancia por inyector de Guice.
     */
    @Override
    protected void configure() {
        bind(PlayerApplicationService.class).in(Singleton.class);
        bind(PlayerService.class).to(PlayerApplicationService.class);
        bind(PlayerRepository.class).to(MongoPlayerRepository.class).in(Singleton.class);
        bind(DeathbanRepository.class).to(RedisDeathbanRepository.class).in(Singleton.class);
        bind(CrateLocationRepository.class).to(MongoCrateLocationRepository.class).in(Singleton.class);
        bind(CrateApplicationService.class).in(Singleton.class);
        bind(CrateService.class).to(CrateApplicationService.class);
        bind(KitApplicationService.class).in(Singleton.class);
        bind(KitService.class).to(KitApplicationService.class);
        bind(CrateInventory.class).in(Singleton.class);
    }
}
