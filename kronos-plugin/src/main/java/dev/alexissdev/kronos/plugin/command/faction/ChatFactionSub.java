package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.plugin.chat.ChatManager;
import dev.alexissdev.kronos.plugin.chat.ChatMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sub-comando {@code /f chat} que permite al jugador alternar entre los modos
 * de chat disponibles: global, facción y aliados. Cada ejecución avanza al
 * siguiente modo en el ciclo gestionado por el {@link ChatManager}, notificando
 * al jugador el modo activo resultante.
 */
@Singleton
public class ChatFactionSub extends FactionSubCommand {

    private final ChatManager    chatManager;
    private final MessagesConfig messages;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param chatManager gestor de modos de chat por jugador
     * @param messages    configuración de mensajes localizados
     */
    @Inject
    public ChatFactionSub(ChatManager chatManager, MessagesConfig messages) {
        this.chatManager = chatManager;
        this.messages    = messages;
    }

    /** @return el nombre del sub-comando: {@code "chat"} */
    @Override public String name() { return "chat"; }

    /**
     * Cicla al siguiente modo de chat del jugador y le notifica el modo resultante.
     * Los modos disponibles son: {@code GLOBAL} → {@code FACTION} → {@code ALLY} → (ciclo).
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos adicionales (no utilizados por este sub-comando)
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
