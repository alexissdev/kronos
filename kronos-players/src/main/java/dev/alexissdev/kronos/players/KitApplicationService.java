package dev.alexissdev.kronos.players;

import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.players.service.KitService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@Singleton
public class KitApplicationService implements KitService {

    @Override
    public void applyKit(PlayerInventory inventory, KitType type) {
        switch (type) {
            case ARCHER:  applyArcherKit(inventory);  break;
            case BARD:    applyBardKit(inventory);    break;
            case ROGUE:   applyRogueKit(inventory);   break;
            case MINER:   applyMinerKit(inventory);   break;
            case KNIGHT:
            case DIAMOND:
            default:      applyKnightKit(inventory);  break;
        }
    }

    private void applyArcherKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        ItemStack bow = enchant(new ItemStack(Material.BOW), Enchantment.ARROW_DAMAGE, 3);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        inv.addItem(bow, new ItemStack(Material.ARROW, 1));
    }

    private void applyBardKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.GOLD_HELMET));
        inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        inv.setBoots(new ItemStack(Material.IRON_BOOTS));
        inv.addItem(new ItemStack(Material.BLAZE_ROD));
    }

    private void applyRogueKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        ItemStack sword = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.DAMAGE_ALL, 3);
        sword.addEnchantment(Enchantment.DURABILITY, 3);
        inv.addItem(sword);
    }

    private void applyMinerKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.IRON_HELMET));
        inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        inv.setBoots(new ItemStack(Material.IRON_BOOTS));
        ItemStack pick = enchant(new ItemStack(Material.DIAMOND_PICKAXE), Enchantment.DIG_SPEED, 5);
        pick.addEnchantment(Enchantment.DURABILITY, 3);
        inv.addItem(pick);
    }

    private void applyKnightKit(PlayerInventory inv) {
        Enchantment prot = Enchantment.PROTECTION_ENVIRONMENTAL;
        inv.setHelmet(enchant(new ItemStack(Material.DIAMOND_HELMET), prot, 2));
        inv.setChestplate(enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), prot, 2));
        inv.setLeggings(enchant(new ItemStack(Material.DIAMOND_LEGGINGS), prot, 2));
        inv.setBoots(enchant(new ItemStack(Material.DIAMOND_BOOTS), prot, 2));
        ItemStack sword = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.DAMAGE_ALL, 4);
        sword.addEnchantment(Enchantment.DURABILITY, 3);
        inv.addItem(sword);
    }

    private static ItemStack enchant(ItemStack item, Enchantment ench, int level) {
        item.addEnchantment(ench, level);
        return item;
    }
}
