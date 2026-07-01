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

@Singleton
public class ChatListener implements Listener {

    private final ChatManager chatManager;
    private final FactionService factionService;
    private final MessagesConfig messages;

    @Inject
    public ChatListener(ChatManager chatManager,
                        FactionService factionService,
                        MessagesConfig messages) {
        this.chatManager = chatManager;
        this.factionService = factionService;
        this.messages = messages;
    }

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
