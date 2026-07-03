package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Comando {@code /fix} que repara completamente el desgaste de todos los
 * ítems del inventario y la armadura del jugador que lo ejecuta.
 * Requiere el permiso {@code hcf.admin} y está pensado para uso administrativo
 * o como beneficio de donador en servidores HCF.
 */
@Singleton
public class FixCommand extends BaseCommand {

    private final MessagesConfig messages;

    /**
     * Construye el comando inyectando las dependencias mediante Guice.
     *
     * @param messages configuración de mensajes localizados
     */
    @Inject
    public FixCommand(MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
    }

    /**
     * Verifica que el ejecutor sea un jugador y, de serlo, repara todos los
     * ítems de su inventario y armadura, notificándole con un mensaje de confirmación.
     *
     * @param sender ejecutor del comando; debe ser un {@link Player}
     * @param args   argumentos adicionales (no utilizados por este comando)
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        repairInventory(player);
        sender.sendMessage(messages.get("fix.done"));
    }

    /**
     * Itera sobre el inventario principal y la armadura del jugador,
     * reparando cada ítem con durabilidad y actualizando la vista del inventario.
     *
     * @param player jugador cuyo inventario será reparado
     */
    private void repairInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            repair(item);
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            repair(item);
        }
        player.updateInventory();
    }

    /**
     * Restablece la durabilidad de un ítem a cero (estado sin desgaste) si el ítem
     * no es nulo y su tipo posee durabilidad máxima mayor que cero.
     *
     * @param item ítem a reparar; si es {@code null} o no tiene durabilidad, la operación es ignorada
     */
    private void repair(ItemStack item) {
        if (item != null && item.getType().getMaxDurability() > 0) {
            item.setDurability((short) 0);
        }
    }
}
