package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.plugin.chat.ChatManager;
import dev.alexissdev.kronos.plugin.chat.ChatMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sub-command {@code /f chat} that allows the player to cycle through the available
 * chat modes: global, faction, and ally. Each execution advances to the next mode
 * in the cycle managed by {@link ChatManager}, and notifies the player of the
 * resulting active mode.
 */
@Singleton
public class ChatFactionSub extends FactionSubCommand {

    private final ChatManager    chatManager;
    private final MessagesConfig messages;

    /**
     * Constructs the sub-command by injecting its dependencies via Guice.
     *
     * @param chatManager per-player chat mode manager
     * @param messages    localised message configuration
     */
    @Inject
    public ChatFactionSub(ChatManager chatManager, MessagesConfig messages) {
        this.chatManager = chatManager;
        this.messages    = messages;
    }

    /** @return the sub-command name: {@code "chat"} */
    @Override public String name() { return "chat"; }

    /**
     * Advances the player to the next chat mode and notifies them of the result.
     * The cycle order is: {@code GLOBAL} → {@code FACTION} → {@code ALLY} → (repeat).
     *
     * @param sender command executor; must be a {@link org.bukkit.entity.Player}
     * @param args   additional arguments (not used by this sub-command)
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        ChatMode next = chatManager.cycleMode(player.getUniqueId());
        switch (next) {
            case FACTION: player.sendMessage(messages.get("chat.mode-faction")); break;
            case ALLY:    player.sendMessage(messages.get("chat.mode-ally"));    break;
            default:      player.sendMessage(messages.get("chat.mode-global"));
        }
    }
}
