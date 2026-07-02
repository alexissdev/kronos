package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.domain.HCFPlayer;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.players.service.PlayerService;
import dev.alexissdev.kronos.timers.TimerApplicationService;
import dev.alexissdev.kronos.timers.domain.TimerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

@Singleton
public class KitCommand extends BaseCommand {

    private static final long KIT_COOLDOWN_MS = 60_000L;

    private final PlayerService playerService;
    private final TimerApplicationService timerService;
    private final MessagesConfig messages;
    private final Plugin plugin;

    @Inject
    public KitCommand(PlayerService playerService, TimerApplicationService timerService,
                      MessagesConfig messages, Plugin plugin) {
        super(null);
        this.playerService = playerService;
        this.timerService  = timerService;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (timerService.hasActiveTimerSync(player.getUniqueId(), TimerType.CLASS_COOLDOWN)) {
            long remaining = timerService.getRemainingSeconds(player.getUniqueId(), TimerType.CLASS_COOLDOWN);
            player.sendMessage(messages.format("kit.cooldown", "seconds", String.valueOf(remaining)));
            return;
        }

        playerService.getOrCreate(player.getUniqueId(), player.getName())
                .thenAccept(hcfPlayer -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        KitType kit = hcfPlayer.getActiveKit();
                        String perm = "hcf.kit." + kit.name().toLowerCase();
                        if (!player.hasPermission(perm)) {
                            player.sendMessage(messages.format("kit.no-permission",
                                    "kit", kit.name()));
                            return;
                        }
                        giveKit(player, hcfPlayer);
                        timerService.startTimer(player.getUniqueId(), TimerType.CLASS_COOLDOWN, KIT_COOLDOWN_MS);
                        player.sendMessage(messages.format("kit.given", "kit", kit.name()));
                    });
                });
    }

    private void giveKit(Player player, HCFPlayer hcfPlayer) {
        KitType kit = hcfPlayer.getActiveKit();
        switch (kit) {
            case ARCHER: giveArcherKit(player); break;
            case BARD:   giveBardKit(player);   break;
            case ROGUE:  giveRogueKit(player);  break;
            case MINER:  giveMinerKit(player);  break;
            case KNIGHT:
            case DIAMOND:
            default:     giveKnightKit(player); break;
        }
    }

    private void giveArcherKit(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        player.getInventory().addItem(bow);
        player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
    }

    private void giveBardKit(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.GOLD_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        player.getInventory().addItem(new ItemStack(Material.BLAZE_ROD));
    }

    private void giveRogueKit(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 3);
        sword.addEnchantment(Enchantment.DURABILITY, 3);
        player.getInventory().addItem(sword);
    }

    private void giveMinerKit(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
        pick.addEnchantment(Enchantment.DIG_SPEED, 5);
        pick.addEnchantment(Enchantment.DURABILITY, 3);
        player.getInventory().addItem(pick);
    }

    private void giveKnightKit(Player player) {
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chest  = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack legs   = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots  = new ItemStack(Material.DIAMOND_BOOTS);
        Enchantment prot = Enchantment.PROTECTION_ENVIRONMENTAL;
        helmet.addEnchantment(prot, 2);
        chest .addEnchantment(prot, 2);
        legs  .addEnchantment(prot, 2);
        boots .addEnchantment(prot, 2);
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chest);
        player.getInventory().setLeggings(legs);
        player.getInventory().setBoots(boots);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 4);
        sword.addEnchantment(Enchantment.DURABILITY, 3);
        player.getInventory().addItem(sword);
    }
}
