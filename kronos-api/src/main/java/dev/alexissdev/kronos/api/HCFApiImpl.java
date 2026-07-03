package dev.alexissdev.kronos.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.facade.*;
import dev.alexissdev.kronos.api.model.*;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.players.domain.HCFPlayer;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.timers.domain.TimerType;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.economy.service.EconomyService;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.koth.service.KothService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Concrete singleton implementation of {@link HCFApi}, managed by the Guice injector
 * and exposed through Bukkit's {@link org.bukkit.plugin.ServicesManager}.
 * <p>
 * Serves as the assembly point for the five HCF facades, delegating each operation
 * to the corresponding domain services via static inner classes. Each domain service
 * is injected by Guice and wrapped in the appropriate facade during object construction.
 * </p>
 * <p>
 * All operations internally resolve the {@link java.util.concurrent.CompletableFuture}
 * instances returned by the domain services via {@code .join()}, making calls blocking
 * from the caller's perspective. Avoid invoking these facades on the main server thread
 * at high frequency to prevent server lag.
 * </p>
 */
@Singleton
public class HCFApiImpl implements HCFApi {

    private final FactionApiFacade factionApi;
    private final PlayerDataApiFacade playerDataApi;
    private final TimerApiFacade timerApi;
    private final ClaimApiFacade claimApi;
    private final KothApiFacade kothApi;

    /**
     * Construye la implementación de la API inyectando todos los servicios de dominio necesarios.
     * <p>
     * Guice gestiona el ciclo de vida de esta instancia como singleton; los servicios
     * se inyectan una única vez y se envuelven en las fachadas correspondientes.
     * </p>
     *
     * @param factionService  servicio de dominio para operaciones sobre facciones
     * @param playerService   servicio de dominio para operaciones sobre jugadores HCF
     * @param economyService  servicio de economía para consultar y modificar balances
     * @param timerService    servicio de aplicación para gestión de temporizadores
     * @param claimService    servicio de dominio para consultas sobre territorios reclamados
     * @param kothService     servicio de dominio para el sistema KOTH
     */
    @Inject
    public HCFApiImpl(FactionService factionService,
                      PlayerService playerService,
                      EconomyService economyService,
                      TimerApplicationService timerService,
                      ClaimService claimService,
                      KothService kothService) {
        this.factionApi = new FactionApiFacade(factionService);
        this.playerDataApi = new PlayerDataApiFacade(playerService, economyService);
        this.timerApi = new TimerApiFacade(timerService);
        this.claimApi = new ClaimApiFacade(claimService);
        this.kothApi = new KothApiFacade(kothService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FactionApi factions() { return factionApi; }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlayerDataApi players() { return playerDataApi; }

    /**
     * {@inheritDoc}
     */
    @Override
    public TimerApi timers() { return timerApi; }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClaimApi claims() { return claimApi; }

    /**
     * {@inheritDoc}
     */
    @Override
    public KothApi koth() { return kothApi; }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() { return "1.0.0-SNAPSHOT"; }

    // ── inner facade implementations ──────────────────────────────────────

    /**
     * Implementación interna de {@link FactionApi} que delega todas las consultas
     * al {@link FactionService} de dominio.
     */
    static class FactionApiFacade implements FactionApi {
        private final FactionService service;

        FactionApiFacade(FactionService service) { this.service = service; }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<FactionSnapshot> getByPlayer(UUID playerUuid) {
            return service.getByPlayer(playerUuid).join().map(HCFApiImpl::toSnapshot);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<FactionSnapshot> getById(String factionId) {
            return service.getById(factionId).join().map(HCFApiImpl::toSnapshot);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<FactionSnapshot> getByName(String name) {
            return service.getByName(name).join().map(HCFApiImpl::toSnapshot);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<FactionSnapshot> getTopFactions(int limit) {
            return service.getTopFactions(limit).join().stream()
                    .map(HCFApiImpl::toSnapshot)
                    .collect(Collectors.toList());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isInFaction(UUID playerUuid) {
            return service.getByPlayer(playerUuid).join().isPresent();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean areAllies(String factionIdA, String factionIdB) {
            return service.getById(factionIdA).join()
                    .map(f -> f.isAlly(factionIdB))
                    .orElse(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean areEnemies(String factionIdA, String factionIdB) {
            return service.getById(factionIdA).join()
                    .map(f -> f.isEnemy(factionIdB))
                    .orElse(false);
        }
    }

    /**
     * Implementación interna de {@link PlayerDataApi} que delega las consultas de estadísticas
     * al {@link PlayerService} y las de balance al {@link EconomyService}.
     */
    static class PlayerDataApiFacade implements PlayerDataApi {
        private final PlayerService service;
        private final EconomyService economyService;

        PlayerDataApiFacade(PlayerService service, EconomyService economyService) {
            this.service = service;
            this.economyService = economyService;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<PlayerSnapshot> getPlayer(UUID uuid) {
            return service.getPlayer(uuid).join().map(p -> {
                double balance = economyService.getBalance(uuid).join();
                return new PlayerSnapshot(p.getUuid(), p.getName(), p.getKills(), p.getDeaths(),
                        balance, Bukkit.getPlayer(uuid) != null);
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getKills(UUID uuid) {
            return service.getPlayer(uuid).join().map(HCFPlayer::getKills).orElse(0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getDeaths(UUID uuid) {
            return service.getPlayer(uuid).join().map(HCFPlayer::getDeaths).orElse(0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getBalance(UUID uuid) {
            return economyService.getBalance(uuid).join();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isOnline(UUID uuid) {
            return Bukkit.getPlayer(uuid) != null;
        }
    }

    /**
     * Implementación interna de {@link TimerApi} que delega todas las consultas de temporizadores
     * al {@link TimerApplicationService}.
     */
    static class TimerApiFacade implements TimerApi {
        private final TimerApplicationService service;

        TimerApiFacade(TimerApplicationService service) { this.service = service; }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasCombatTag(UUID uuid) {
            return service.hasActiveTimer(uuid, TimerType.COMBAT_TAG).join();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasPvpTimer(UUID uuid) {
            return service.hasActiveTimer(uuid, TimerType.PVP_TIMER).join();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasEnderpearlCooldown(UUID uuid) {
            return service.hasActiveTimer(uuid, TimerType.ENDERPEARL).join();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OptionalLong getRemainingMillis(UUID uuid, TimerType type) {
            return service.getRemainingMillis(uuid, type).join();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<dev.alexissdev.kronos.api.model.TimerSnapshot> getTimer(UUID uuid, TimerType type) {
            return service.getRemainingMillis(uuid, type).join()
                    .stream()
                    .mapToObj(ms -> new dev.alexissdev.kronos.api.model.TimerSnapshot(
                            uuid, type,
                            java.time.Instant.now().plusMillis(ms), ms))
                    .findFirst();
        }
    }

    /**
     * Implementación interna de {@link ClaimApi} que delega todas las consultas de territorios
     * al {@link ClaimService}.
     */
    static class ClaimApiFacade implements ClaimApi {
        private final ClaimService service;

        ClaimApiFacade(ClaimService service) { this.service = service; }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<ClaimSnapshot> getClaimAt(World world, int chunkX, int chunkZ) {
            return service.getClaimAt(world.getName(), chunkX, chunkZ).join()
                    .map(c -> new ClaimSnapshot(c.getId(), c.getFactionId(), c.getType(),
                            c.getWorld(), c.getMinChunkX(), c.getMinChunkZ(),
                            c.getMaxChunkX(), c.getMaxChunkZ()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClaimType getClaimTypeAt(Location location) {
            return service.getClaimTypeAt(location.getWorld().getName(),
                    location.getChunk().getX(), location.getChunk().getZ()).join();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isClaimed(World world, int chunkX, int chunkZ) {
            return service.getClaimAt(world.getName(), chunkX, chunkZ).join().isPresent();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isWilderness(Location location) {
            return getClaimTypeAt(location) == ClaimType.WILDERNESS;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSafeZone(Location location) {
            return getClaimTypeAt(location) == ClaimType.SAFEZONE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isWarZone(Location location) {
            return getClaimTypeAt(location) == ClaimType.WARZONE;
        }
    }

    /**
     * Implementación interna de {@link KothApi} que delega todas las consultas del sistema
     * KOTH al {@link KothService}.
     */
    static class KothApiFacade implements KothApi {
        private final KothService service;

        KothApiFacade(KothService service) { this.service = service; }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<KothSnapshot> getActiveKoths() {
            return service.getActiveKoths().join().stream()
                    .map(z -> new KothSnapshot(z.getName(), z.getWorld(), z.getMinX(), z.getMinZ(),
                            z.getMaxX(), z.getMaxZ(), z.isActive(), z.getRewardCrateType()))
                    .collect(Collectors.toList());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<KothSnapshot> getKoth(String name) {
            return service.getKoth(name).join()
                    .map(z -> new KothSnapshot(z.getName(), z.getWorld(), z.getMinX(), z.getMinZ(),
                            z.getMaxX(), z.getMaxZ(), z.isActive(), z.getRewardCrateType()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isKothActive(String name) {
            return service.getKoth(name).join().map(KothZone::isActive).orElse(false);
        }
    }

    /**
     * Convierte una entidad de dominio {@link Faction} en una {@link FactionSnapshot} inmutable.
     * <p>
     * Extrae todos los datos relevantes de la facción, incluyendo la lista de miembros
     * y sus roles, para construir una vista de solo lectura segura para consumo externo.
     * </p>
     *
     * @param f entidad de dominio {@link Faction} a convertir
     * @return {@link FactionSnapshot} con los datos actuales de la facción
     */
    private static FactionSnapshot toSnapshot(Faction f) {
        List<UUID> members = new ArrayList<>(f.getMembers().keySet());
        Map<UUID, String> roles = new LinkedHashMap<>();
        f.getMembers().forEach((uuid, member) -> roles.put(uuid, member.getRole().name()));
        return new FactionSnapshot(f.getId(), f.getName(), f.getLeaderId(), members, roles,
                f.getKills(), f.getDeaths(), f.getDtkRemaining(), f.getBalance(), f.getCreatedAt());
    }
}
