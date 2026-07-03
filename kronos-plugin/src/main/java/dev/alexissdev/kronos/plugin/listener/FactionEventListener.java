package dev.alexissdev.kronos.plugin.listener;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.api.event.FactionCreateEvent;
import dev.alexissdev.kronos.api.event.FactionDisbandEvent;
import dev.alexissdev.kronos.api.event.FactionClaimEvent;
import dev.alexissdev.kronos.api.event.PlayerJoinFactionEvent;
import dev.alexissdev.kronos.api.event.PlayerLeaveFactionEvent;
import dev.alexissdev.kronos.api.model.ClaimSnapshot;
import dev.alexissdev.kronos.api.model.FactionSnapshot;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.event.FactionCreatedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDisbandedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionDtkDecrementedDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionRaidableDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerJoinedFactionDomainEvent;
import dev.alexissdev.kronos.factions.event.PlayerLeftFactionDomainEvent;
import dev.alexissdev.kronos.factions.event.FactionClaimedDomainEvent;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.factions.service.FactionService;
import dev.alexissdev.kronos.plugin.tablist.TabListManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener que actúa como puente entre los eventos de dominio internos del sistema de facciones
 * y el sistema de eventos de Bukkit (API pública).
 *
 * <p>Se suscribe al {@link EventBus} de Guava para recibir eventos de dominio emitidos por la
 * capa de aplicación (creación, disolución, unión y salida de facciones, reclamaciones de territorio,
 * cambios en DTK y estado de saqueable) y los traduce en eventos de Bukkit que otros plugins
 * pueden escuchar. Además, actualiza el TabList y emite mensajes de broadcast cuando corresponde.
 *
 * <p>Dado que los eventos de dominio pueden originarse en hilos asíncronos, todas las acciones
 * sobre la API de Bukkit se delegan al hilo principal mediante el scheduler.
 */
@Singleton
public class FactionEventListener implements Listener {

    private final Plugin plugin;
    private final FactionService factionService;
    private final EventBus eventBus;
    private final MessagesConfig messages;
    private final TabListManager tabListManager;

    /**
     * Crea el listener y lo registra automáticamente en el {@link EventBus} para recibir eventos
     * de dominio desde la capa de aplicación de facciones.
     *
     * @param plugin          instancia del plugin principal, usada para programar tareas en el hilo principal
     * @param factionService  servicio de consulta de facciones
     * @param eventBus        bus de eventos de Guava donde se publican los eventos de dominio
     * @param messages        configuración de mensajes localizada
     * @param tabListManager  gestor del TabList que debe actualizarse al cambiar la membresía de una facción
     */
    @Inject
    public FactionEventListener(Plugin plugin, FactionService factionService,
                                 EventBus eventBus, MessagesConfig messages,
                                 TabListManager tabListManager) {
        this.plugin = plugin;
        this.factionService = factionService;
        this.eventBus = eventBus;
        this.messages = messages;
        this.tabListManager = tabListManager;
        this.eventBus.register(this);
    }

    /**
     * Traduce el evento de dominio de creación de facción en un evento de Bukkit
     * ({@link dev.alexissdev.kronos.api.event.FactionCreateEvent}) y lo publica en el servidor.
     *
     * <p>Construye un {@link dev.alexissdev.kronos.api.model.FactionSnapshot} con los datos
     * iniciales de la facción recién creada y lo encapsula en el evento de Bukkit.
     *
     * @param event evento de dominio emitido tras la creación exitosa de una facción
     */
    @Subscribe
    public void onFactionCreated(FactionCreatedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            FactionSnapshot snapshot = new FactionSnapshot(
                    event.getFactionId(), event.getFactionName(), event.getLeaderId(),
                    List.of(event.getLeaderId()), Map.of(event.getLeaderId(), "LEADER"),
                    0, 0, 20, 0.0, Instant.now()
            );
            FactionCreateEvent bukkitEvent = new FactionCreateEvent(snapshot);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });
    }

    /**
     * Traduce el evento de dominio de disolución de facción en un evento de Bukkit
     * ({@link dev.alexissdev.kronos.api.event.FactionDisbandEvent}), emite un broadcast a todos
     * los jugadores en línea y refresca el TabList de todos los jugadores (los ex-miembros
     * pasan al formato sin facción).
     *
     * @param event evento de dominio emitido tras la disolución exitosa de una facción
     */
    @Subscribe
    public void onFactionDisbanded(FactionDisbandedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            FactionDisbandEvent bukkitEvent = new FactionDisbandEvent(
                    event.getFactionId(), event.getFactionName(), event.getActorUuid());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            String msg = messages.format("faction.broadcast.disbanded", "name", event.getFactionName());
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(msg);
            // Reset tab names for all online players (former members get no-faction format)
            tabListManager.refreshAll();
        });
    }

    /**
     * Traduce el evento de dominio de unión de un jugador a una facción en un evento de Bukkit
     * ({@link dev.alexissdev.kronos.api.event.PlayerJoinFactionEvent}) y actualiza el TabList
     * del jugador para reflejar su nueva afiliación.
     *
     * @param event evento de dominio emitido cuando un jugador se une a una facción
     */
    @Subscribe
    public void onPlayerJoined(PlayerJoinedFactionDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerJoinFactionEvent bukkitEvent = new PlayerJoinFactionEvent(
                    event.getPlayerUuid(), event.getFactionId());
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            tabListManager.refresh(event.getPlayerUuid());
        });
    }

    /**
     * Traduce el evento de dominio de salida de un jugador de una facción en un evento de Bukkit
     * ({@link dev.alexissdev.kronos.api.event.PlayerLeaveFactionEvent}), actualiza el TabList
     * del jugador y notifica a los miembros restantes de la facción indicando si el jugador
     * salió voluntariamente o fue expulsado.
     *
     * @param event evento de dominio emitido cuando un jugador abandona o es expulsado de una facción
     */
    @Subscribe
    public void onPlayerLeft(PlayerLeftFactionDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerLeaveFactionEvent bukkitEvent = new PlayerLeaveFactionEvent(
                    event.getPlayerUuid(), event.getFactionId(), event.wasKicked());
            Bukkit.getPluginManager().callEvent(bukkitEvent);

            tabListManager.refresh(event.getPlayerUuid());

            Player player = Bukkit.getPlayer(event.getPlayerUuid());
            if (player != null) {
                String reason = event.wasKicked() ? "expulsado" : "salió";
                factionService.getById(event.getFactionId()).thenAccept(opt ->
                        opt.ifPresent(f -> Bukkit.getScheduler().runTask(plugin, () ->
                                f.getMembers().values().stream()
                                        .map(m -> Bukkit.getPlayer(m.getUuid()))
                                        .filter(p -> p != null)
                                        .forEach(p -> p.sendMessage(messages.format(
                                                "faction.member.left",
                                                "player", player.getName(),
                                                "reason", reason))))));
            }
        });
    }

    /**
     * Notifica a los miembros en línea de la facción cuando su contador DTK
     * (Deaths To Kill — muertes necesarias para volver la facción saqueable) se decrementa.
     *
     * <p>El mensaje incluye el nombre de la facción, el nuevo valor de DTK y el máximo posible.
     *
     * @param event evento de dominio emitido cada vez que un miembro de la facción muere a manos
     *              de un jugador de otra facción
     */
    @Subscribe
    public void onDtkDecremented(FactionDtkDecrementedDomainEvent event) {
        factionService.getById(event.getFactionId()).thenAccept(opt ->
                opt.ifPresent(f -> Bukkit.getScheduler().runTask(plugin, () -> {
                    String msg = messages.format("faction.dtk-decremented",
                            "faction", event.getFactionName(),
                            "dtk",     String.valueOf(event.getNewDtk()),
                            "max",     String.valueOf(event.getMaxDtk()));
                    for (UUID memberUuid : f.getMembers().keySet()) {
                        Player member = Bukkit.getPlayer(memberUuid);
                        if (member != null) member.sendMessage(msg);
                    }
                })));
    }

    /**
     * Emite un broadcast global cuando una facción se vuelve saqueable (raidable).
     *
     * <p>Una facción es saqueable cuando su contador DTK llega a cero, lo que permite a
     * otros jugadores teletransportarse a su territorio usando perlas de éter y acceder
     * a sus recursos.
     *
     * @param event evento de dominio emitido cuando el DTK de la facción alcanza cero
     */
    @Subscribe
    public void onFactionRaidable(FactionRaidableDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = messages.format("faction.broadcast.raidable", "name", event.getFactionName());
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(msg);
        });
    }

    /**
     * Traduce el evento de dominio de reclamación de territorio de una facción en un evento
     * de Bukkit ({@link dev.alexissdev.kronos.api.event.FactionClaimEvent}) que otros plugins
     * pueden escuchar.
     *
     * @param event evento de dominio emitido cuando una facción reclama exitosamente un chunk
     */
    @Subscribe
    public void onFactionClaimed(FactionClaimedDomainEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            ClaimSnapshot snapshot = new ClaimSnapshot(
                            event.getClaimId(), event.getFactionId(),
                            ClaimType.valueOf(event.getClaimType()), event.getWorld(),
                            event.getMinChunkX(), event.getMinChunkZ(),
                            event.getMaxChunkX(), event.getMaxChunkZ());
            FactionClaimEvent bukkitEvent = new FactionClaimEvent(event.getFactionId(), snapshot);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
        });
    }
}
