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
 * Gestor central del sistema de scoreboards del plugin HCF Kronos.
 * <p>
 * Mantiene un mapa de {@link PlayerBoard} (representación Bukkit del marcador) y
 * {@link PlayerBoardData} (snapshot de datos) indexados por UUID de jugador.
 * Coordina las actualizaciones periódicas con {@link ScoreboardTask} y reacciona
 * a eventos del dominio (timers de jugador y ciclo de vida de KOTHs) a través
 * del EventBus de Guava.
 * </p>
 * <p>
 * Las estadísticas lentas (kills, deaths, balance, facción) se refrescan de forma
 * asíncrona cada 5 segundos para evitar bloquear el hilo principal del servidor.
 * Los conteos de timers y el estado SOTW/EOTW se actualizan en el hilo principal
 * cada segundo para garantizar que la cuenta regresiva del marcador sea precisa.
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
     * Construye el gestor de scoreboards inyectando sus dependencias y registrándose
     * en el EventBus de Guava para recibir eventos de timers y KOTH.
     *
     * @param eventBus       bus de eventos compartido por todos los módulos del plugin
     * @param renderer       renderizador que genera las líneas del marcador lateral
     * @param plugin         instancia principal del plugin, necesaria para programar
     *                       tareas en el scheduler de Bukkit
     * @param playerService  servicio para consultar las estadísticas del jugador (kills/deaths)
     * @param factionService servicio para consultar la facción y los DTK restantes del jugador
     * @param economyService servicio para consultar el balance económico del jugador
     * @param sotwService    servicio que provee el tiempo restante de SOTW y EOTW globales
     * @param messages       configuración de mensajes y plantillas de texto del plugin
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
     * Inicializa el marcador lateral de un jugador recién conectado al servidor.
     * <p>
     * Crea un {@link PlayerBoardData} vacío, instancia el {@link PlayerBoard} que
     * asigna el scoreboard al jugador en Bukkit y lanza el primer refresco asíncrono
     * de sus estadísticas.
     * </p>
     *
     * @param player jugador que acaba de unirse al servidor
     */
    public void createBoard(Player player) {
        PlayerBoardData data = new PlayerBoardData();
        cache.put(player.getUniqueId(), data);
        boards.put(player.getUniqueId(), new PlayerBoard(player, TITLE));
        refreshStats(player.getUniqueId());
    }

    /**
     * Elimina el marcador lateral y los datos cacheados de un jugador que se desconecta,
     * liberando los recursos asociados en los mapas internos del gestor.
     *
     * @param player jugador que abandona el servidor
     */
    public void removeBoard(Player player) {
        boards.remove(player.getUniqueId());
        cache.remove(player.getUniqueId());
    }

    // ── periodic updates ──────────────────────────────────────────────────

    /**
     * Actualiza todos los scoreboards desde el hilo principal del servidor.
     * <p>
     * Obtiene los tiempos restantes de SOTW y EOTW, los propaga a cada
     * {@link PlayerBoardData} y redibuja el marcador de todos los jugadores en línea.
     * Adicionalmente, detecta la transición de SOTW/EOTW de activo a inactivo para
     * enviar el mensaje de finalización a todo el servidor.
     * </p>
     * <p>
     * Este método es invocado cada segundo por {@link ScoreboardTask}.
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
     * Refresca de forma asíncrona las estadísticas lentas de todos los jugadores en línea.
     * <p>
     * Consulta kills, deaths, balance económico, nombre de facción y DTK para cada jugador
     * mediante sus respectivos servicios. Al ejecutarse en un hilo asíncrono, no bloquea
     * el tick del servidor. Los datos se escriben en {@link PlayerBoardData} con campos
     * {@code volatile} para garantizar visibilidad en el hilo principal.
     * </p>
     * <p>
     * Este método es invocado cada 5 segundos por {@link ScoreboardTask}.
     * </p>
     */
    public void refreshAllStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshStats(player.getUniqueId());
        }
    }

    // ── EventBus: per-player timers ────────────────────────────────────────

    /**
     * Reacciona al inicio de un timer individual de jugador (ej. combat tag, PvP timer,
     * enderpearl, gapple, etc.).
     * <p>
     * Calcula el timestamp de expiración y lo almacena en el {@link PlayerBoardData}
     * del jugador. Luego programa un redibujado inmediato del scoreboard en el hilo
     * principal para que el timer aparezca sin esperar al siguiente tick.
     * </p>
     *
     * @param event evento con el UUID del jugador, el tipo de timer y su duración en milisegundos
     */
    @Subscribe
    public void onTimerStarted(PlayerTimerStartedDomainEvent event) {
        PlayerBoardData data = cache.get(event.getPlayerUuid());
        if (data == null) return;
        data.setTimer(event.getTimerType(), System.currentTimeMillis() + event.getDurationMillis());
        Bukkit.getScheduler().runTask(plugin, () -> redraw(event.getPlayerUuid()));
    }

    /**
     * Reacciona al vencimiento de un timer individual de jugador.
     * <p>
     * Elimina el timer del {@link PlayerBoardData} del jugador y fuerza un redibujado
     * inmediato del scoreboard en el hilo principal para que la línea del timer
     * desaparezca del marcador sin esperar al siguiente tick.
     * </p>
     *
     * @param event evento con el UUID del jugador y el tipo de timer que expiró
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
     * Reacciona al inicio de un evento KOTH en el servidor.
     * <p>
     * Crea una {@link KothEntry} con los datos del KOTH y la registra en el mapa de
     * KOTHs activos. A continuación ejecuta un {@link #tickAll()} inmediato en el hilo
     * principal para que el KOTH aparezca en el marcador de todos los jugadores sin demora.
     * </p>
     *
     * @param event evento con el nombre, las coordenadas del centro y el tiempo de captura del KOTH
     */
    @Subscribe
    public void onKothStarted(KothStartedDomainEvent event) {
        activeKoths.put(event.getKothName(), new KothEntry(
                event.getKothName(), event.getCenterX(),
                event.getCenterZ(), event.getCaptureTimeSeconds()));
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    /**
     * Reacciona a la captura exitosa de un KOTH por parte de una facción.
     * <p>
     * Elimina el KOTH del mapa de activos y actualiza los scoreboards de todos
     * los jugadores para que la entrada del KOTH desaparezca del marcador lateral.
     * </p>
     *
     * @param event evento con el nombre del KOTH que fue capturado
     */
    @Subscribe
    public void onKothCaptured(KothCapturedDomainEvent event) {
        activeKoths.remove(event.getKothName());
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    /**
     * Reacciona al fin de un KOTH sin que haya sido capturado (tiempo agotado).
     * <p>
     * Elimina el KOTH del mapa de activos y actualiza los scoreboards de todos
     * los jugadores para reflejar que el KOTH ya no está disponible.
     * </p>
     *
     * @param event evento con el nombre del KOTH que finalizó sin ser capturado
     */
    @Subscribe
    public void onKothEnded(KothEndedDomainEvent event) {
        activeKoths.remove(event.getKothName());
        Bukkit.getScheduler().runTask(plugin, this::tickAll);
    }

    // ── KOTH capture progress (called from KothListener) ─────────────────

    /**
     * Actualiza el progreso de captura de KOTH para un jugador específico.
     * <p>
     * Llamado desde el listener de KOTH cada vez que el jugador permanece dentro
     * de la zona de captura. El dato se almacena en su {@link PlayerBoardData} para
     * que el {@link ScoreboardRenderer} muestre la cuenta regresiva personalizada.
     * </p>
     *
     * @param uuid        UUID del jugador que está capturando el KOTH
     * @param kothName    nombre del KOTH en proceso de captura
     * @param remainingMs milisegundos restantes para completar la captura
     */
    public void updateKothCapture(UUID uuid, String kothName, long remainingMs) {
        PlayerBoardData data = cache.get(uuid);
        if (data == null) return;
        data.setKothCapture(kothName, remainingMs);
    }

    /**
     * Elimina el progreso de captura de KOTH registrado para un jugador.
     * <p>
     * Llamado cuando el jugador sale de la zona de captura o el KOTH finaliza,
     * de modo que el marcador del jugador deje de mostrar la cuenta regresiva
     * de captura personalizada.
     * </p>
     *
     * @param uuid UUID del jugador cuyo progreso de captura debe eliminarse
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
