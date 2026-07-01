package dev.alexissdev.kronos.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.facade.*;
import dev.alexissdev.kronos.api.model.*;
import dev.alexissdev.kronos.core.domain.*;
import dev.alexissdev.kronos.core.service.*;
import dev.alexissdev.kronos.application.timer.TimerApplicationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class HCFApiImpl implements HCFApi {

    private final FactionApiFacade factionApi;
    private final PlayerDataApiFacade playerDataApi;
    private final TimerApiFacade timerApi;
    private final ClaimApiFacade claimApi;
    private final KothApiFacade kothApi;

    @Inject
    public HCFApiImpl(FactionService factionService,
                      PlayerService playerService,
                      EconomyService economyService,
                      TimerApplicationService timerService,
                      ClaimService claimService,
                      KotHService kothService) {
        this.factionApi = new FactionApiFacade(factionService);
        this.playerDataApi = new PlayerDataApiFacade(playerService, economyService);
        this.timerApi = new TimerApiFacade(timerService);
        this.claimApi = new ClaimApiFacade(claimService);
        this.kothApi = new KothApiFacade(kothService);
    }

    @Override
    public FactionApi factions() { return factionApi; }

    @Override
    public PlayerDataApi players() { return playerDataApi; }

    @Override
    public TimerApi timers() { return timerApi; }

    @Override
    public ClaimApi claims() { return claimApi; }

    @Override
    public KothApi koth() { return kothApi; }

    @Override
    public String getVersion() { return "1.0.0-SNAPSHOT"; }

    // ── inner facade implementations ──────────────────────────────────────

    static class FactionApiFacade implements FactionApi {
        private final FactionService service;

        FactionApiFacade(FactionService service) { this.service = service; }

        @Override
        public Optional<FactionSnapshot> getByPlayer(UUID playerUuid) {
            return service.getByPlayer(playerUuid).join().map(HCFApiImpl::toSnapshot);
        }

        @Override
        public Optional<FactionSnapshot> getById(String factionId) {
            return service.getById(factionId).join().map(HCFApiImpl::toSnapshot);
        }

        @Override
        public Optional<FactionSnapshot> getByName(String name) {
            return service.getByName(name).join().map(HCFApiImpl::toSnapshot);
        }

        @Override
        public List<FactionSnapshot> getTopFactions(int limit) {
            return service.getTopFactions(limit).join().stream()
                    .map(HCFApiImpl::toSnapshot)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isInFaction(UUID playerUuid) {
            return service.getByPlayer(playerUuid).join().isPresent();
        }

        @Override
        public boolean areAllies(String factionIdA, String factionIdB) {
            return service.getById(factionIdA).join()
                    .map(f -> f.isAlly(factionIdB))
                    .orElse(false);
        }

        @Override
        public boolean areEnemies(String factionIdA, String factionIdB) {
            return service.getById(factionIdA).join()
                    .map(f -> f.isEnemy(factionIdB))
                    .orElse(false);
        }
    }

    static class PlayerDataApiFacade implements PlayerDataApi {
        private final PlayerService service;
        private final EconomyService economyService;

        PlayerDataApiFacade(PlayerService service, EconomyService economyService) {
            this.service = service;
            this.economyService = economyService;
        }

        @Override
        public Optional<PlayerSnapshot> getPlayer(UUID uuid) {
            return service.getPlayer(uuid).join().map(p -> {
                double balance = economyService.getBalance(uuid).join();
                return new PlayerSnapshot(p.getUuid(), p.getName(), p.getKills(), p.getDeaths(),
                        balance, Bukkit.getPlayer(uuid) != null);
            });
        }

        @Override
        public int getKills(UUID uuid) {
            return service.getPlayer(uuid).join().map(HCFPlayer::getKills).orElse(0);
        }

        @Override
        public int getDeaths(UUID uuid) {
            return service.getPlayer(uuid).join().map(HCFPlayer::getDeaths).orElse(0);
        }

        @Override
        public double getBalance(UUID uuid) {
            return economyService.getBalance(uuid).join();
        }

        @Override
        public boolean isOnline(UUID uuid) {
            return Bukkit.getPlayer(uuid) != null;
        }
    }

    static class TimerApiFacade implements TimerApi {
        private final TimerApplicationService service;

        TimerApiFacade(TimerApplicationService service) { this.service = service; }

        @Override
        public boolean hasCombatTag(UUID uuid) {
            return service.hasActiveTimer(uuid, TimerType.COMBAT_TAG).join();
        }

        @Override
        public boolean hasPvpTimer(UUID uuid) {
            return service.hasActiveTimer(uuid, TimerType.PVP_TIMER).join();
        }

        @Override
        public boolean hasEnderpearlCooldown(UUID uuid) {
            return service.hasActiveTimer(uuid, TimerType.ENDERPEARL).join();
        }

        @Override
        public OptionalLong getRemainingMillis(UUID uuid, TimerType type) {
            return service.getRemainingMillis(uuid, type).join();
        }

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

    static class ClaimApiFacade implements ClaimApi {
        private final ClaimService service;

        ClaimApiFacade(ClaimService service) { this.service = service; }

        @Override
        public Optional<ClaimSnapshot> getClaimAt(World world, int chunkX, int chunkZ) {
            return service.getClaimAt(world.getName(), chunkX, chunkZ).join()
                    .map(c -> new ClaimSnapshot(c.getId(), c.getFactionId(), c.getType(),
                            c.getWorld(), c.getMinChunkX(), c.getMinChunkZ(),
                            c.getMaxChunkX(), c.getMaxChunkZ()));
        }

        @Override
        public ClaimType getClaimTypeAt(Location location) {
            return service.getClaimTypeAt(location.getWorld().getName(),
                    location.getChunk().getX(), location.getChunk().getZ()).join();
        }

        @Override
        public boolean isClaimed(World world, int chunkX, int chunkZ) {
            return service.getClaimAt(world.getName(), chunkX, chunkZ).join().isPresent();
        }

        @Override
        public boolean isWilderness(Location location) {
            return getClaimTypeAt(location) == ClaimType.WILDERNESS;
        }

        @Override
        public boolean isSafeZone(Location location) {
            return getClaimTypeAt(location) == ClaimType.SAFEZONE;
        }

        @Override
        public boolean isWarZone(Location location) {
            return getClaimTypeAt(location) == ClaimType.WARZONE;
        }
    }

    static class KothApiFacade implements KothApi {
        private final KotHService service;

        KothApiFacade(KotHService service) { this.service = service; }

        @Override
        public List<KothSnapshot> getActiveKoths() {
            return service.getActiveKoths().join().stream()
                    .map(z -> new KothSnapshot(z.getName(), z.getWorld(), z.getMinX(), z.getMinZ(),
                            z.getMaxX(), z.getMaxZ(), z.isActive(), z.getRewardCrateType()))
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<KothSnapshot> getKoth(String name) {
            return service.getKoth(name).join()
                    .map(z -> new KothSnapshot(z.getName(), z.getWorld(), z.getMinX(), z.getMinZ(),
                            z.getMaxX(), z.getMaxZ(), z.isActive(), z.getRewardCrateType()));
        }

        @Override
        public boolean isKothActive(String name) {
            return service.getKoth(name).join().map(KothZone::isActive).orElse(false);
        }
    }

    private static FactionSnapshot toSnapshot(Faction f) {
        List<UUID> members = new ArrayList<>(f.getMembers().keySet());
        Map<UUID, String> roles = new LinkedHashMap<>();
        f.getMembers().forEach((uuid, member) -> roles.put(uuid, member.getRole().name()));
        return new FactionSnapshot(f.getId(), f.getName(), f.getLeaderId(), members, roles,
                f.getKills(), f.getDeaths(), f.getDtkRemaining(), f.getBalance(), f.getCreatedAt());
    }
}
