package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Command {@code /fix} that fully restores the durability of every item in the
 * executing player's inventory and armor slots. Requires the {@code hcf.admin}
 * permission and is intended for administrative use or as a donor perk on HCF
 * servers.
 */
@Singleton
public class FixCommand extends BaseCommand {

    private final MessagesConfig messages;

    /**
     * Constructs the command by injecting its dependencies via Guice.
     *
     * @param messages localised message configuration
     */
    @Inject
    public FixCommand(MessagesConfig messages) {
        super("hcf.admin");
        this.messages = messages;
    }

    /**
     * Verifies that the executor is a player and, if so, repairs all items in their
     * inventory and armour slots, then notifies them with a confirmation message.
     *
     * @param sender command executor; must be a {@link Player}
     * @param args   additional arguments (not used by this command)
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        repairInventory(player);
        sender.sendMessage(messages.get("fix.done"));
    }

    /**
     * Iterates over the player's main inventory and armour contents, repairing
     * every durable item and then refreshing the inventory view for the client.
     *
     * @param player player whose inventory will be repaired
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
     * Resets the durability of an item to zero (no wear) if the item is non-null
     * and its material type has a max durability greater than zero.
     *
     * @param item item to repair; if {@code null} or not damageable, the operation is silently skipped
     */
    private void repair(ItemStack item) {
        if (item != null && item.getType().getMaxDurability() > 0) {
            item.setDurability((short) 0);
        }
    }
}
