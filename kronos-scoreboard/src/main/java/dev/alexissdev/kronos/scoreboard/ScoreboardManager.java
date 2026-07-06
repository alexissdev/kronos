package dev.alexissdev.kronos.scoreboard;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.SotwService;
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
import java.util.Map;

/**
 * Central manager for the scoreboard system in the HCF Kronos plugin.
 * <p>
 * Maintains a map of {@link PlayerBoard} (the Bukkit scoreboard wrapper) and
 * {@link PlayerBoardData} (the data snapshot), both indexed by player UUID.
 * Coordinates periodic updates with {@link ScoreboardTask} and reacts to
 * domain events (player timers and KOTH lifecycle) through Guava's EventBus.
 * </p>
 * <p>
 * Slow statistics (kills, deaths, balance, faction) are refreshed asynchronously
 * every 5 seconds to avoid blocking the server's main thread. Timer countdowns
 * and SOTW/EOTW state are updated on the main thread every second to keep the
 * scoreboard countdown accurate.
 * </p>
 */
@Singleton
public class ScoreboardManager {

    private static final String TITLE = "§e§lKRONOS HCF";

    private final Map<UUID, PlayerBoard>     boards      = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerBoardData> cache       = new ConcurrentHashMap<>();
    private final Map<String, KothEntry>     activeKoths = new ConcurrentHashMap<>();

    private final ScoreboardRenderer renderer;
    private final JavaPlugin plugin;
    private final PlayerService playerService;
    private final FactionService factionService;
    private final EconomyService economyService;
    private final SotwService sotwService;
    private final MessagesConfig messages;

    private volatile boolean lastSotwActive = false;
    private volatile boolean lastEotwActive = false;

    /**
     * Constructs the scoreboard manager by injecting its dependencies and registering
     * itself on Guava's EventBus to receive timer and KOTH events.
     *
     * @param eventBus       the event bus shared by all modules of the plugin
     * @param renderer       the renderer that builds the sidebar lines for each player
     * @param plugin         the main plugin instance, needed to schedule tasks on Bukkit's scheduler
     * @param playerService  service for querying player statistics (kills / deaths)
     * @param factionService service for querying the player's faction and remaining DTK
     * @param economyService service for querying the player's economic balance
     * @param sotwService    service that provides the global remaining SOTW and EOTW times
     * @param messages       plugin message configuration and text templates
     */
    @Inject
    public ScoreboardManager(EventBus eventBus,
                             ScoreboardRenderer renderer,
                             JavaPlugin plugin,
                             PlayerService playerService,
                             FactionService factionService,
                             EconomyService economyService,
                             SotwService sotwService,
                             MessagesConfig messages) {
        this.renderer       = renderer;
        this.plugin         = plugin;
        this.playerService  = playerService;
        this.factionService = factionService;
        this.economyService = economyService;
        this.sotwService    = sotwService;
        this.messages       = messages;
        eventBus.register(this);
    }

    // ── lifecycle ─────────────────────────────────────────────────────────

    /**
     * Initialises the sidebar scoreboard for a player who has just joined the server.
     * <p>
     * Creates an empty {@link PlayerBoardData}, instantiates the {@link PlayerBoard}
     * that assigns the Bukkit scoreboard to the player, and triggers the first
     * asynchronous stats refresh for that player.
     * </p>
     *
     * @param player the player who just joined the server
     */
    public void createBoard(Player player) {
        PlayerBoardData data = new PlayerBoardData();
        cache.put(player.getUniqueId(), data);
        boards.put(player.getUniqueId(), new PlayerBoard(player, TITLE));
        refreshStats(player.getUniqueId());
    }

    /**
     * Removes the sidebar scoreboard and cached data of a disconnecting player,
     * freeing the resources held in the manager's internal maps.
     *
     * @param player the player who is leaving the server
     */
    public void removeBoard(Player player) {
        boards.remove(player.getUniqueId());
        cache.remove(player.getUniqueId());
    }

    // ── periodic updates ──────────────────────────────────────────────────

    /**
     * Updates all scoreboards from the server's main thread.
     * <p>
     * Retrieves the current SOTW and EOTW remaining times, propagates them to every
     * {@link PlayerBoardData}, and redraws the scoreboard for all online players.
     * Additionally detects the transition from an active SOTW/EOTW to an inactive one
     * and broadcasts the corresponding end message to the entire server.
     * </p>
     * <p>
     * This method is invoked every second by {@link ScoreboardTask}.
     * </p>
     */
    public void tickAll() {
        long sotwMs = sotwService.getSotwRemainingMs();
        long eotwMs = sotwService.getEotwRemainingMs();
        boolean sotwNow = sotwMs > 0;
        boolean eotwNow = eotwMs > 0;

        if (lastSotwActive && !sotwNow) {
            String msg = messages.get("sotw.ended");
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(msg);
        }
        if (lastEotwActive && !eotwNow) {
            String msg = messages.get("eotw.ended");
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(msg);
        }
        lastSotwActive = sotwNow;
        lastEotwActive = eotwNow;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerBoardData data = cache.get(player.getUniqueId());
            if (data != null) {
                data.setSotwRemainingMs(sotwMs);
                data.setEotwRemainingMs(eotwMs);
            }
            redraw(player.getUniqueId());
        }
    }

    /**
     * Asynchronously refreshes the slow statistics of all online players.
     * <p>
     * Queries kills, deaths, economic balance, faction name, and DTK for each
     * player through their respective services. Running on an async thread ensures
     * the server's main tick is never blocked. Data is written to {@link PlayerBoardData}
     * via {@code volatile} fields to guarantee visibility on the main thread.
     * </p>
     * <p>
     * This method is invoked every 5 seconds by {@link ScoreboardTask}.
     * </p>
     */
    public void refreshAllStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshStats(player.getUniqueId());
        }
    }

    // ── EventBus: per-player timers ────────────────────────────────────────

    /**
     * Reacts to the start of a per-player timer (e.g. combat tag, PvP timer,
     * enderpearl, gapple, etc.).
     * <p>
     * Calculates the expiry timestamp and stores it in the player's {@link PlayerBoardData}.
     * Then schedules an immediate scoreboard redraw on the main thread so the timer
     * appears without waiting for the next tick.
     * </p>
     *
     * @param event event carrying the player's UUID, the timer type, and its duration in milliseconds
     */
    @Subscribe
    public void onTimerStarted(PlayerTimerStartedDomainEvent event) {
        PlayerBoardData data = cache.get(event.getPlayerUuid());
        if (data == null) return;
        data.setTimer(event.getTimerType(), System.currentTimeMillis() + event.getDurationMillis());
        Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
    }

    /**
     * Reacts to the expiry of a per-player timer.
     * <p>
     * Removes the timer from the player's {@link PlayerBoardData} and forces an
     * immediate scoreboard redraw on the main thread so the timer line disappears
     * from the sidebar without waiting for the next tick.
     * </p>
     *
     * @param event event carrying the player's UUID and the timer type that expired
     */
    @Subscribe
    public void onTimerExpired(PlayerTimerExpiredDomainEvent event) {
        PlayerBoardData data = cache.get(event.getPlayerUuid());
        if (data == null) return;
        data.clearTimer(event.getTimerType());
        Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
    }

    // ── EventBus: KOTH ─────────────────────────────────────────────────────

    /**
     * Reacts to the start of a KOTH event on the server.
     * <p>
     * Creates a {@link KothEntry} with the KOTH data and registers it in the
     * active-KOTH map. Then immediately executes {@link #tickAll()} on the main
     * thread so the KOTH appears on all players' scoreboards without delay.
     * </p>
     *
     * @param event event carrying the KOTH name, centre coordinates, and capture time
     */
    @Subscribe
    public void onKothStarted(KothStartedDomainEvent event) {
        activeKoths.put(event.getKothName(), new KothEntry(
                event.getKothName(), event.getCenterX(),
                event.getCenterZ(), event.getCaptureTimeSeconds()));
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    /**
     * Reacts to the successful capture of a KOTH by a faction.
     * <p>
     * Removes the KOTH from the active map and updates all players' scoreboards
     * so the KOTH entry disappears from the sidebar.
     * </p>
     *
     * @param event event carrying the name of the KOTH that was captured
     */
    @Subscribe
    public void onKothCaptured(KothCapturedDomainEvent event) {
        activeKoths.remove(event.getKothName());
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    /**
     * Reacts to a KOTH ending without being captured (time ran out).
     * <p>
     * Removes the KOTH from the active map and updates all players' scoreboards
     * to reflect that the KOTH is no longer available.
     * </p>
     *
     * @param event event carrying the name of the KOTH that ended without being captured
     */
    @Subscribe
    public void onKothEnded(KothEndedDomainEvent event) {
        activeKoths.remove(event.getKothName());
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    // ── KOTH capture progress (called from KothListener) ─────────────────

    /**
     * Updates the KOTH capture progress for a specific player.
     * <p>
     * Called from the KOTH listener each time the player remains inside the capture zone.
     * The data is stored in their {@link PlayerBoardData} so that {@link ScoreboardRenderer}
     * can display a personalised countdown.
     * </p>
     *
     * @param uuid        UUID of the player who is capturing the KOTH
     * @param kothName    name of the KOTH being captured
     * @param remainingMs milliseconds remaining to complete the capture
     */
    public void updateKothCapture(UUID uuid, String kothName, long remainingMs) {
        PlayerBoardData data = cache.get(uuid);
        if (data == null) return;
        data.setKothCapture(kothName, remainingMs);
    }

    /**
     * Clears the KOTH capture progress recorded for a player.
     * <p>
     * Called when the player leaves the capture zone or the KOTH ends,
     * so the player's scoreboard stops showing the personalised capture countdown.
     * </p>
     *
     * @param uuid UUID of the player whose capture progress should be removed
     */
    public void clearKothCapture(UUID uuid) {
        PlayerBoardData data = cache.get(uuid);
        if (data == null) return;
        data.clearKothCapture();
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
