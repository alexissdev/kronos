package dev.alexissdev.kronos.players;

import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.domain.KitType;
import dev.alexissdev.kronos.players.service.KitService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Servicio de aplicación que implementa la lógica de aplicación de kits de combate
 * a inventarios de jugadores en el servidor HCF.
 *
 * <p>Implementa {@link KitService} y provee el equipamiento específico para cada clase
 * de combate disponible. Cuando un jugador selecciona un kit, este servicio coloca la
 * armadura y las armas correspondientes directamente en su inventario de Bukkit.</p>
 *
 * <p>Los kits disponibles son: ARCHER (arco y armadura ligera), BARD (soporte con
 * varilla de blaze), ROGUE (espada de diamante y armadura de malla), MINER (pico de
 * diamante eficiente), y KNIGHT/DIAMOND (armadura y espada de diamante con encantamientos).</p>
 *
 * <p>Esta clase es un singleton gestionado por Guice y forma parte del módulo {@link PlayersModule}.</p>
 */
@Singleton
public class KitApplicationService implements KitService {

    /**
     * {@inheritDoc}
     *
     * <p>Delega en el método privado correspondiente al tipo de kit. Los tipos
     * {@link KitType#KNIGHT} y {@link KitType#DIAMOND} son tratados de la misma
     * manera (equipamiento idéntico de diamante con Protección II).</p>
     */
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

    /**
     * Aplica el kit de Arquero al inventario: armadura de cuero/malla y arco con
     * Poder III y Flecha Infinita, más una flecha de munición.
     *
     * @param inv inventario del jugador donde se colocan los ítems
     */
    private void applyArcherKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        ItemStack bow = enchant(new ItemStack(Material.BOW), Enchantment.ARROW_DAMAGE, 3);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        inv.addItem(bow, new ItemStack(Material.ARROW, 1));
    }

    /**
     * Aplica el kit de Bardo al inventario: armadura mixta de oro y hierro,
     * más una varilla de blaze para aplicar efectos de buff a aliados.
     *
     * @param inv inventario del jugador donde se colocan los ítems
     */
    private void applyBardKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.GOLD_HELMET));
        inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        inv.setBoots(new ItemStack(Material.IRON_BOOTS));
        inv.addItem(new ItemStack(Material.BLAZE_ROD));
    }

    /**
     * Aplica el kit de Pícaro al inventario: armadura completa de malla y espada de
     * diamante con Filo III y Durabilidad III para combate rápido y sigiloso.
     *
     * @param inv inventario del jugador donde se colocan los ítems
     */
    private void applyRogueKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        ItemStack sword = enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.DAMAGE_ALL, 3);
        sword.addEnchantment(Enchantment.DURABILITY, 3);
        inv.addItem(sword);
    }

    /**
     * Aplica el kit de Minero al inventario: armadura completa de hierro y pico de
     * diamante con Eficiencia V y Durabilidad III para la recolección eficiente de recursos.
     *
     * @param inv inventario del jugador donde se colocan los ítems
     */
    private void applyMinerKit(PlayerInventory inv) {
        inv.setHelmet(new ItemStack(Material.IRON_HELMET));
        inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        inv.setBoots(new ItemStack(Material.IRON_BOOTS));
        ItemStack pick = enchant(new ItemStack(Material.DIAMOND_PICKAXE), Enchantment.DIG_SPEED, 5);
        pick.addEnchantment(Enchantment.DURABILITY, 3);
        inv.addItem(pick);
    }

    /**
     * Aplica el kit de Caballero (o Diamante) al inventario: armadura completa de diamante
     * con Protección II en cada pieza y espada de diamante con Filo IV y Durabilidad III.
     * Es la clase más resistente para el combate frontal sostenido.
     *
     * @param inv inventario del jugador donde se colocan los ítems
     */
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

    /**
     * Aplica un encantamiento a un ítem de Bukkit y lo devuelve para permitir el encadenamiento.
     *
     * @param item  ítem al que se le aplica el encantamiento
     * @param ench  encantamiento a aplicar
     * @param level nivel del encantamiento a aplicar
     * @return el mismo ítem con el encantamiento aplicado
     */
    private static ItemStack enchant(ItemStack item, Enchantment ench, int level) {
        item.addEnchantment(ench, level);
        return item;
    }
}
