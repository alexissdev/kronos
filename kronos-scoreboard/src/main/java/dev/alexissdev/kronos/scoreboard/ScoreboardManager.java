package dev.alexissdev.kronos.scoreboard;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.koth.event.KothCapturedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothEndedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothStartedDomainEvent;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.timers.event.PlayerTimerExpiredDomainEvent;
import dev.alexissdev.kronos.timers.event.PlayerTimerStartedDomainEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ScoreboardManager {

    private static final String TITLE = "§e§lKRONOS HCF";

    private final ConcurrentHashMap<UUID, PlayerBoard>     boards      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PlayerBoardData> cache       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, KothEntry>     activeKoths = new ConcurrentHashMap<>();

    private final ScoreboardRenderer renderer;
    private final JavaPlugin plugin;
    private final PlayerService playerService;
    private final FactionService factionService;
    private final EconomyService economyService;

    @Inject
    public ScoreboardManager(EventBus eventBus,
                             ScoreboardRenderer renderer,
                             JavaPlugin plugin,
                             PlayerService playerService,
                             FactionService factionService,
                             EconomyService economyService) {
        this.renderer       = renderer;
        this.plugin         = plugin;
        this.playerService  = playerService;
        this.factionService = factionService;
        this.economyService = economyService;
        eventBus.register(this);
    }

    // ── lifecycle ─────────────────────────────────────────────────────────

    public void createBoard(Player player) {
        PlayerBoardData data = new PlayerBoardData();
        cache.put(player.getUniqueId(), data);
        boards.put(player.getUniqueId(), new PlayerBoard(player, TITLE));
        refreshStats(player.getUniqueId());
    }

    public void removeBoard(Player player) {
        boards.remove(player.getUniqueId());
        cache.remove(player.getUniqueId());
    }

    // ── periodic updates ──────────────────────────────────────────────────

    /** Called every second from the main thread — redraws all boards (timer countdown). */
    public void tickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            redraw(player.getUniqueId());
        }
    }

    /** Called every 5 s from an async thread — refreshes kills/faction/balance. */
    public void refreshAllStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshStats(player.getUniqueId());
        }
    }

    // ── EventBus: per-player timers ────────────────────────────────────────

    @Subscribe
    public void onTimerStarted(PlayerTimerStartedDomainEvent event) {
        PlayerBoardData data = cache.get(event.getPlayerUuid());
        if (data == null) return;
        data.setTimer(event.getTimerType(), System.currentTimeMillis() + event.getDurationMillis());
        Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
    }

    @Subscribe
    public void onTimerExpired(PlayerTimerExpiredDomainEvent event) {
        PlayerBoardData data = cache.get(event.getPlayerUuid());
        if (data == null) return;
        data.clearTimer(event.getTimerType());
        Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
    }

    // ── EventBus: KOTH ─────────────────────────────────────────────────────

    @Subscribe
    public void onKothStarted(KothStartedDomainEvent event) {
        activeKoths.put(event.getKothName(), new KothEntry(
                event.getKothName(), event.getCenterX(),
                event.getCenterZ(), event.getCaptureTimeSeconds()));
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    @Subscribe
    public void onKothCaptured(KothCapturedDomainEvent event) {
        activeKoths.remove(event.getKothName());
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    @Subscribe
    public void onKothEnded(KothEndedDomainEvent event) {
        activeKoths.remove(event.getKothName());
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    // ── internals ─────────────────────────────────────────────────────────

    private void redraw(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        PlayerBoard board = boards.get(uuid);
        PlayerBoardData data = cache.get(uuid);
        if (player == null || board == null || data == null) return;

        Collection<KothEntry> koths = activeKoths.values();
        List<String> lines = renderer.render(player, data, koths);
        board.render(lines);
    }

    private void refreshStats(UUID uuid) {
        PlayerBoardData data = cache.get(uuid);
        if (data == null) return;

        playerService.getPlayer(uuid).thenAccept(opt -> opt.ifPresent(p -> {
            data.setKills(p.getKills());
            data.setDeaths(p.getDeaths());
        }));

        economyService.getBalance(uuid).thenAccept(data::setBalance);

        factionService.getByPlayer(uuid).thenAccept(opt -> {
            if (opt.isPresent()) {
                data.setFactionName(opt.get().getName());
                data.setDtkRemaining(opt.get().getDtkRemaining());
            } else {
                data.setFactionName(null);
                data.setDtkRemaining(0);
            }
        });
    }
}
