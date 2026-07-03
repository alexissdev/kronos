package dev.alexissdev.kronos.players.inventory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.domain.CrateType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Componente de interfaz gráfica que gestiona la animación de apertura de crates
 * mediante un inventario de Bukkit de 27 slots (3 filas).
 *
 * <p>Cuando un jugador abre un crate, se le muestra un inventario con un borde de
 * cristal gris y una animación de giro en la fila central durante 30 ticks. Al finalizar,
 * se selecciona un premio aleatorio según la tabla de recompensas del tipo de crate,
 * se le entrega al jugador y se le notifica con un mensaje en el chat.</p>
 *
 * <p>La animación se ejecuta de forma asíncrona usando el planificador de tareas de Bukkit
 * ({@link BukkitRunnable}) para no bloquear el hilo principal. Esta clase es un singleton
 * gestionado por Guice y forma parte del módulo {@link dev.alexissdev.kronos.players.PlayersModule}.</p>
 */
@Singleton
public class CrateInventory {

    private static final int SIZE = 27;

    private final Plugin plugin;

    /**
     * Crea la clase de inventario de crates con la instancia del plugin inyectada por Guice.
     * El plugin es necesario para programar las tareas del planificador de Bukkit.
     *
     * @param plugin instancia principal del plugin de Bukkit
     */
    @Inject
    public CrateInventory(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre la animación de apertura del crate para el jugador especificado.
     *
     * <p>Crea un inventario de 27 slots con el nombre del tipo de crate, llena el borde
     * con cristales de colores y lanza la animación de giro en la fila central. Tras
     * 30 ticks de animación, se selecciona el premio final y se entrega al jugador.</p>
     *
     * @param player    jugador de Bukkit que abre el crate y verá la animación
     * @param crateType tipo de crate que determina la tabla de recompensas a mostrar
     */
    public void openCrateAnimation(Player player, CrateType crateType) {
        Inventory inv = Bukkit.createInventory(null, SIZE,
                ChatColor.GOLD + "Crate: " + crateType.name());

        fillBorderWithGlass(inv);
        startSpinAnimation(player, inv, crateType);
    }

    /**
     * Rellena todos los slots del inventario con paneles de cristal gris para crear
     * el efecto de borde decorativo alrededor de la fila central de animación.
     *
     * @param inv inventario de Bukkit a rellenar con el borde de cristal
     */
    private void fillBorderWithGlass(Inventory inv) {
        // data value 7 = gray in 1.8.8 stained glass pane
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GRAY + " ");
        glass.setItemMeta(meta);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, glass);
        }
    }

    /**
     * Inicia la animación de giro que muestra premios aleatorios en la fila central del inventario.
     *
     * <p>Ejecuta un {@link BukkitRunnable} que rota premios aleatorios cada 3 ticks durante
     * un máximo de 30 ticks. Si el jugador se desconecta durante la animación, ésta se cancela
     * sin entregar ningún premio. Al finalizar, se muestra el premio ganado en el slot central
     * (slot 13) y se entrega al inventario del jugador 40 ticks después.</p>
     *
     * @param player    jugador que está viendo la animación
     * @param inv       inventario donde se reproduce la animación
     * @param crateType tipo de crate para seleccionar premios de la tabla correcta
     */
    private void startSpinAnimation(Player player, Inventory inv, CrateType crateType) {
        player.openInventory(inv);
        final int maxTicks = 30;

        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || tick >= maxTicks) {
                    cancel();
                    if (player.isOnline()) {
                        ItemStack prize = selectPrize(crateType);
                        inv.setItem(13, prize);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.closeInventory();
                            player.getInventory().addItem(prize);
                            player.sendMessage(ChatColor.GOLD + "¡Obtuviste: " +
                                    ChatColor.WHITE + prize.getItemMeta().getDisplayName() + ChatColor.GOLD + "!");
                        }, 40L);
                    }
                    return;
                }
                randomizeMiddleRow(inv, crateType);
                tick++;
            }
        }.runTaskTimer(plugin, 5L, 3L);
    }

    /**
     * Rellena los 9 slots de la fila central del inventario con premios aleatorios,
     * creando el efecto visual de ruleta durante la animación de giro.
     *
     * @param inv       inventario donde se actualizan los slots centrales
     * @param crateType tipo de crate para determinar qué premios mostrar en la animación
     */
    private void randomizeMiddleRow(Inventory inv, CrateType crateType) {
        int[] middleSlots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
        for (int slot : middleSlots) {
            inv.setItem(slot, selectPrize(crateType));
        }
    }

    /**
     * Selecciona aleatoriamente un premio de la tabla de recompensas del tipo de crate dado.
     *
     * @param crateType tipo de crate cuya tabla de premios se usará para la selección
     * @return ítem de Bukkit seleccionado aleatoriamente como premio
     */
    private ItemStack selectPrize(CrateType crateType) {
        List<ItemStack> prizes = getPrizesFor(crateType);
        return prizes.get(new Random().nextInt(prizes.size()));
    }

    /**
     * Construye y devuelve la tabla de premios posibles para el tipo de crate especificado.
     *
     * <p>Cada tipo de crate tiene su propia selección de ítems temáticos:
     * KOTH otorga ítems de combate de alto nivel, VOTE otorga recursos básicos,
     * RANK otorga ítems de rango especial y EVENT otorga ítems exclusivos de eventos.</p>
     *
     * @param crateType tipo de crate cuya tabla de premios se quiere obtener
     * @return lista de ítems posibles como premio para este tipo de crate
     */
    private List<ItemStack> getPrizesFor(CrateType crateType) {
        List<ItemStack> prizes = new ArrayList<>();
        switch (crateType) {
            case KOTH:
                prizes.add(createItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Espada KOTH"));
                prizes.add(createItem(Material.GOLDEN_APPLE, ChatColor.GOLD + "Manzana Dorada"));
                prizes.add(createItem(Material.DIAMOND, ChatColor.AQUA + "x5 Diamantes"));
                prizes.add(createItem(Material.EXP_BOTTLE, ChatColor.GREEN + "XP Bottle"));
                break;
            case VOTE:
                prizes.add(createItem(Material.IRON_INGOT, ChatColor.GRAY + "x10 Hierro"));
                prizes.add(createItem(Material.GOLD_INGOT, ChatColor.GOLD + "x5 Oro"));
                prizes.add(createItem(Material.DIAMOND, ChatColor.AQUA + "x2 Diamantes"));
                prizes.add(createItem(Material.EMERALD, ChatColor.GREEN + "x3 Esmeraldas"));
                break;
            case RANK:
                prizes.add(createItem(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Rango VIP"));
                prizes.add(createItem(Material.DIAMOND_CHESTPLATE, ChatColor.AQUA + "Pechera Diamante"));
                break;
            case EVENT:
                prizes.add(createItem(Material.NETHER_STAR, ChatColor.YELLOW + "Estrella del Nether"));
                prizes.add(createItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE + "Libro Encantado"));
                prizes.add(createItem(Material.IRON_CHESTPLATE, ChatColor.WHITE + "Pechera de Hierro"));
                break;
        }
        return prizes;
    }

    /**
     * Crea un ítem de Bukkit con el material y nombre para mostrar indicados.
     *
     * @param material material del ítem a crear
     * @param name     nombre para mostrar en el ítem (con colores de chat de Bukkit)
     * @return ítem de Bukkit con el metadata configurado
     */
    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

}
