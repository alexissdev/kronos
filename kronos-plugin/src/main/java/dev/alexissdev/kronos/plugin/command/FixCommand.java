package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Singleton
public class FixCommand extends BaseCommand {

    private final MessagesConfig messages;

    @Inject
    public FixCommand(MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        repairInventory(player);
        sender.sendMessage(messages.get("fix.done"));
    }

    private void repairInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            repair(item);
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            repair(item);
        }
        player.updateInventory();
    }

    private void repair(ItemStack item) {
        if (item != null && item.getType().getMaxDurability() > 0) {
            item.setDurability((short) 0);
        }
    }
}
