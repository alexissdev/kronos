package dev.alexissdev.kronos.plugin.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Bukkit listener that intercepts player chat messages and routes them to the correct
 * channel based on the sender's active {@link ChatMode}.
 *
 * <p>This listener cancels the native Bukkit chat event ({@link org.bukkit.event.player.AsyncPlayerChatEvent})
 * and manually dispatches the message to the appropriate set of recipients:
 * <ul>
 *   <li>{@link ChatMode#GLOBAL} — all online players.</li>
 *   <li>{@link ChatMode#FACTION} — only the members of the sender's faction.</li>
 *   <li>{@link ChatMode#ALLY} — the members of the sender's faction and all of its allied factions.</li>
 * </ul>
 *
 * <p>The faction lookup is performed asynchronously so that the event thread is never blocked.
 */
@Singleton
public class ChatListener implements Listener {

    private final ChatManager chatManager;
    private final FactionService factionService;
    private final MessagesConfig messages;

    /**
     * Crea el listener con todas sus dependencias inyectadas por Guice.
     *
     * @param chatManager    gestor que mantiene el modo de chat activo de cada jugador
     * @param factionService servicio para consultar la facción a la que pertenece el jugador
     * @param messages       configuración de mensajes localizada del plugin
     */
    @Inject
    public ChatListener(ChatManager chatManager,
                        FactionService factionService,
                        MessagesConfig messages) {
        this.chatManager = chatManager;
        this.factionService = factionService;
        this.messages = messages;
    }

    /**
     * Intercepta el evento de chat asíncrono del jugador y lo redirige al canal correcto.
     *
     * <p>El evento se cancela siempre para evitar que Bukkit distribuya el mensaje por defecto.
     * Acto seguido se consulta la facción del remitente de forma asíncrona y, una vez resuelta,
     * se envía el mensaje al grupo de destinatarios correspondiente al modo activo.
     *
     * @param event evento de chat asíncrono de Bukkit, ya filtrado por prioridad alta
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String rawMessage = event.getMessage();
        ChatMode mode = chatManager.getMode(player.getUniqueId());

        factionService.getByPlayer(player.getUniqueId()).thenAccept(factionOpt -> {
            Faction faction = factionOpt.orElse(null);
            switch (mode) {
                case FACTION: sendFactionMessage(player, faction, rawMessage); break;
                case ALLY:    sendAllyMessage(player, faction, rawMessage);    break;
                default:      sendGlobalMessage(player, faction, rawMessage);
            }
        });
    }

    /**
     * Restablece el modo de chat del jugador a {@link ChatMode#GLOBAL} cuando se desconecta.
     *
     * <p>Esto libera la entrada correspondiente del mapa interno de {@link ChatManager} y garantiza
     * que la próxima vez que el jugador se conecte parta con el modo predeterminado.
     *
     * @param event evento de desconexión del jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        chatManager.reset(event.getPlayer().getUniqueId());
    }

    // ── senders ────────────────────────────────────────────────────────────

    private void sendGlobalMessage(Player sender, Faction faction, String rawMessage) {
        String formatted = faction != null
                ? messages.format("chat.global-format",
                        "faction", faction.getName(),
                        "player", sender.getName(),
                        "message", rawMessage)
                : messages.format("chat.global-no-faction",
                        "player", sender.getName(),
                        "message", rawMessage);

        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(formatted));
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private void sendFactionMessage(Player sender, Faction faction, String rawMessage) {
        if (faction == null) {
            sender.sendMessage(messages.get("chat.no-faction"));
            chatManager.reset(sender.getUniqueId());
            return;
        }
        String formatted = messages.format("chat.faction-format",
                "player", sender.getName(),
                "message", rawMessage);

        dispatchToFaction(faction, formatted);
        Bukkit.getConsoleSender().sendMessage(formatted);
    }

    private void sendAllyMessage(Player sender, Faction faction, String rawMessage) {
        if (faction == null) {
            sender.sendMessage(messages.get("chat.no-faction"));
            chatManager.reset(sender.getUniqueId());
            return;
        }
        String formatted = messages.format("chat.ally-format",
                "faction", faction.getName(),
                "player", sender.getName(),
                "message", rawMessage);

        // Own faction members
        dispatchToFaction(faction, formatted);

        // Allied faction members
        List<CompletableFuture<Void>> futures = faction.getAllies().stream()
                .map(id -> factionService.getById(id)
                        .thenAccept(opt -> opt.ifPresent(ally -> dispatchToFaction(ally, formatted))))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> Bukkit.getConsoleSender().sendMessage(formatted));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static void dispatchToFaction(Faction faction, String formatted) {
        faction.getMembers().keySet().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) member.sendMessage(formatted);
        });
    }
}
