package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.KothCaptureEvent;
import dev.alexissdev.kronos.api.event.KothStartEvent;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.event.KothCapturedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothDeletedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothEndedDomainEvent;
import dev.alexissdev.kronos.koth.event.KothStartedDomainEvent;
import dev.alexissdev.kronos.koth.service.KothService;
import dev.alexissdev.kronos.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener central que gestiona toda la mecánica de los eventos KOTH (King of the Hill).
 *
 * <p>Un KOTH es una zona especial del mapa donde los jugadores compiten por capturar un punto
 * de control permaneciendo dentro de él durante el tiempo configurado. Este listener se encarga de:
 * <ul>
 *   <li>Mantener cachés en memoria de todas las zonas KOTH registradas y de las activas en curso.</li>
 *   <li>Detectar cuándo un jugador entra o sale de la zona exterior de un KOTH y enviarle mensajes.</li>
 *   <li>Detectar cuándo un jugador entra en la zona de captura de un KOTH activo e iniciar su progreso.</li>
 *   <li>Ejecutar una tarea asíncrona que verifica el progreso de captura cada segundo y confirma
 *       la captura cuando se cumple el tiempo requerido.</li>
 *   <li>Reaccionar a eventos de dominio ({@link com.google.common.eventbus.EventBus}) para actualizar
 *       las cachés cuando un KOTH inicia, es capturado, termina o es eliminado.</li>
 * </ul>
 *
 * <p>Al ser capturado, el jugador recibe una llave de crate de tipo KOTH
 * ({@link dev.alexissdev.kronos.common.domain.CrateType#KOTH}).
 */
@Singleton
public class KothListener implements Listener {

    private final KothService kothService;
    private final Plugin plugin;
    private final MessagesConfig messages;
    private final ScoreboardManager scoreboardManager;

    /** Caché de todas las zonas KOTH registradas, usada para mensajes de entrada/salida. */
    private final Map<String, KothZone> allKothCache    = new ConcurrentHashMap<>();
    /** Caché únicamente de las zonas KOTH activas, usada para la lógica de captura. */
    private final Map<String, KothZone> activeKothCache = new ConcurrentHashMap<>();

    /** Zona exterior en la que se encuentra actualmente cada jugador ({@code null} si no está en ninguna). */
    private final Map<UUID, String> playerOuterZone = new ConcurrentHashMap<>();

    /** Nombre del KOTH que está capturando actualmente cada jugador. */
    private final Map<UUID, String> capturingKoth = new ConcurrentHashMap<>();
    /** Instante en milisegundos en que cada jugador comenzó a capturar. */
    private final Map<UUID, Long>   captureStart  = new ConcurrentHashMap<>();

    /**
     * Crea el listener, lo registra en el {@link com.google.common.eventbus.EventBus}, carga
     * todas las zonas KOTH desde la base de datos en la caché y arranca la tarea de verificación
     * de captura.
     *
     * @param kothService       servicio de dominio de KOTH para consultar zonas y registrar capturas
     * @param plugin            instancia del plugin principal para programar tareas
     * @param eventBus          bus de eventos de Guava para recibir eventos de dominio de KOTH
     * @param messages          configuración de mensajes localizada
     * @param scoreboardManager gestor del scoreboard para actualizar el progreso de captura
     */
    @Inject
    public KothListener(KothService kothService, Plugin plugin, EventBus eventBus,
                        MessagesConfig messages, ScoreboardManager scoreboardManager) {
        this.kothService        = kothService;
        this.plugin             = plugin;
        this.messages           = messages;
        this.scoreboardManager  = scoreboardManager;
        eventBus.register(this);

        kothService.getAllKoths().thenAccept(zones -> {
            for (KothZone z : zones) {
                allKothCache.put(z.getName(), z);
                if (z.isActive()) activeKothCache.put(z.getName(), z);
            }
        });

        startCaptureTask();
    }

    // ── Domain events → update caches ─────────────────────────────────────

    /**
     * Actualiza la caché cuando un KOTH inicia, lo agrega a la caché de activos,
     * publica el evento de Bukkit {@link dev.alexissdev.kronos.api.event.KothStartEvent}
     * y emite un broadcast si el evento no es cancelado.
     *
     * @param event evento de dominio emitido cuando un KOTH comienza a estar activo
     */
    @Subscribe
    public void onKothStarted(KothStartedDomainEvent event) {
        KothZone zone = event.getZone();
        allKothCache.put(zone.getName(), zone);
        activeKothCache.put(zone.getName(), zone);

        Bukkit.getScheduler().runTask(plugin, () -> {
            KothStartEvent bukkitEvent = new KothStartEvent(event.getKothName());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            if (!bukkitEvent.isCancelled()) {
                String msg = messages.format("koth.broadcast.started", "name", event.getKothName());
                for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(msg);
            }
        });
    }

    /**
     * Maneja la captura exitosa de un KOTH: elimina la zona de la caché de activos, expulsa
     * a todos los jugadores en proceso de captura, publica el evento de Bukkit
     * {@link dev.alexissdev.kronos.api.event.KothCaptureEvent}, emite un broadcast
     * y entrega una llave de crate KOTH al jugador capturador.
     *
     * @param event evento de dominio que incluye el nombre del KOTH y el UUID del capturador
     */
    @Subscribe
    public void onKothCaptured(KothCapturedDomainEvent event) {
        activeKothCache.remove(event.getKothName());
        evictCapturingPlayers(event.getKothName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            KothCaptureEvent bukkitEvent = new KothCaptureEvent(event.getKothName(), event.getCaptorUuid());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            Player captor = Bukkit.getPlayer(event.getCaptorUuid());
            String captorName = captor != null ? captor.getName() : "Unknown";
            String msg = messages.format("koth.broadcast.captured",
                    "player", captorName, "name", event.getKothName());
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(msg);
            if (captor != null) {
                captor.getInventory().addItem(CrateListener.createKey(CrateType.KOTH));
                captor.sendMessage(messages.get("koth.reward-key"));
            }
        });
    }

    /**
     * Maneja la finalización de un KOTH sin captura exitosa: elimina la zona de la caché de
     * activos y cancela el estado de captura de todos los jugadores que estaban en proceso.
     *
     * @param event evento de dominio emitido cuando un KOTH termina sin que nadie lo haya capturado
     */
    @Subscribe
    public void onKothEnded(KothEndedDomainEvent event) {
        activeKothCache.remove(event.getKothName());
        evictCapturingPlayers(event.getKothName());
    }

    /**
     * Elimina un KOTH de todas las cachés cuando es borrado del sistema, cancelando también el
     * estado de captura de los jugadores afectados y limpiando el rastreo de zona exterior.
     *
     * @param event evento de dominio emitido cuando un administrador elimina un KOTH del sistema
     */
    @Subscribe
    public void onKothDeleted(KothDeletedDomainEvent event) {
        allKothCache.remove(event.getKothName());
        activeKothCache.remove(event.getKothName());
        evictCapturingPlayers(event.getKothName());
        playerOuterZone.entrySet().removeIf(e -> event.getKothName().equals(e.getValue()));
    }

    // ── Player movement ───────────────────────────────────────────────────

    /**
     * Rastrea el movimiento del jugador para detectar entradas y salidas de zonas KOTH y
     * actualizar el estado de captura.
     *
     * <p>Solo se procesa si el jugador ha cruzado al menos un bloque completo (no sub-píxel)
     * para optimizar el rendimiento. La lógica se divide en dos capas:
     * <ol>
     *   <li><strong>Zona exterior</strong> (todos los KOTHs): envía mensajes de entrada/salida.</li>
     *   <li><strong>Zona de captura</strong> (solo KOTHs activos): inicia o cancela el proceso
     *       de captura del jugador.</li>
     * </ol>
     *
     * @param event evento de movimiento del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hasCrossedBlock(event)) return;
        if (allKothCache.isEmpty()) return;

        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();
        String world  = event.getTo().getWorld().getName();
        double x      = event.getTo().getX();
        double z      = event.getTo().getZ();

        // -- Outer zone: enter/leave messages (all KOTHs, active or not) --
        KothZone newOuter = null;
        for (KothZone kz : allKothCache.values()) {
            if (kz.containsLocation(world, x, z)) { newOuter = kz; break; }
        }
        String prevOuterName = playerOuterZone.get(uuid);
        String newOuterName  = newOuter != null ? newOuter.getName() : null;

        if (!Objects.equals(prevOuterName, newOuterName)) {
            if (newOuterName != null) {
                player.sendMessage(messages.format("koth.entered-zone", "name", newOuterName));
                playerOuterZone.put(uuid, newOuterName);
            } else {
                player.sendMessage(messages.format("koth.left-zone", "name", prevOuterName));
                playerOuterZone.remove(uuid);
            }
        }

        // -- Capture zone: only active KOTHs --
        KothZone captureZone = null;
        for (KothZone kz : activeKothCache.values()) {
            if (kz.isInCaptureZone(world, x, z)) { captureZone = kz; break; }
        }

        if (captureZone != null) {
            boolean isNew = captureStart.putIfAbsent(uuid, System.currentTimeMillis()) == null;
            capturingKoth.put(uuid, captureZone.getName());
            if (isNew) {
                long remainingMs = (long) captureZone.getCaptureTimeSeconds() * 1000L;
                String captureMsg = messages.format("koth.started-capturing",
                        "player", player.getName(),
                        "name", captureZone.getName());
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(captureMsg);
                }
                scoreboardManager.updateKothCapture(uuid, captureZone.getName(), remainingMs);
            }
        } else {
            if (capturingKoth.remove(uuid) != null) {
                captureStart.remove(uuid);
                scoreboardManager.clearKothCapture(uuid);
            }
        }
    }

    /**
     * Limpia todos los datos de rastreo de KOTH del jugador cuando se desconecta.
     *
     * <p>Elimina su zona exterior registrada, su progreso de captura y actualiza el scoreboard
     * para evitar fugas de memoria y estados inconsistentes.
     *
     * @param event evento de desconexión del jugador
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerOuterZone.remove(uuid);
        capturingKoth.remove(uuid);
        captureStart.remove(uuid);
        scoreboardManager.clearKothCapture(uuid);
    }

    // ── Capture tick ──────────────────────────────────────────────────────

    private void startCaptureTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, String> entry : new ConcurrentHashMap<>(capturingKoth).entrySet()) {
                UUID   playerUuid = entry.getKey();
                String kothName   = entry.getValue();

                KothZone zone = activeKothCache.get(kothName);
                if (zone == null || !zone.isActive()) {
                    capturingKoth.remove(playerUuid);
                    captureStart.remove(playerUuid);
                    continue;
                }

                long startTime  = captureStart.getOrDefault(playerUuid, System.currentTimeMillis());
                long elapsed    = System.currentTimeMillis() - startTime;
                long requiredMs = (long) zone.getCaptureTimeSeconds() * 1000L;

                long remainingMs = requiredMs - elapsed;

                if (elapsed >= requiredMs) {
                    capturingKoth.remove(playerUuid);
                    captureStart.remove(playerUuid);
                    scoreboardManager.clearKothCapture(playerUuid);
                    kothService.captureKoth(kothName, playerUuid)
                            .exceptionally(ex -> {
                                plugin.getLogger().warning("[KOTH] Error al capturar " + kothName + ": " + ex.getMessage());
                                return null;
                            });
                } else {
                    scoreboardManager.updateKothCapture(playerUuid, kothName, remainingMs);
                }
            }
        }, 20L, 20L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void evictCapturingPlayers(String kothName) {
        capturingKoth.entrySet().removeIf(e -> {
            if (kothName.equals(e.getValue())) {
                scoreboardManager.clearKothCapture(e.getKey());
                return true;
            }
            return false;
        });
        captureStart.entrySet().removeIf(e -> !capturingKoth.containsKey(e.getKey()));
    }

    private boolean hasCrossedBlock(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}
