package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.domain.CrateLocation;
import dev.alexissdev.kronos.players.inventory.CrateInventory;
import dev.alexissdev.kronos.players.service.CrateService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener que gestiona la interacción de los jugadores con los cofres de recompensas (crates).
 *
 * <p>Mantiene una caché en memoria de las ubicaciones de crates registradas en la base de datos,
 * indexadas por la clave {@code "mundo:x:y:z"}, para detectar de forma eficiente cuándo un
 * jugador hace clic derecho sobre un bloque que corresponde a un crate.
 *
 * <p>Cuando un jugador interactúa con un crate, el listener verifica que tenga en la mano
 * una llave de crate válida ({@link #createKey(CrateType)}) y del tipo correcto, consume la
 * llave y abre la animación de recompensas.
 */
@Singleton
public class CrateListener implements Listener {

    private static final String LORE_PREFIX = ChatColor.DARK_GRAY + "crate:";

    // key: "world:x:y:z" → CrateType
    private final Map<String, CrateType> crateCache = new ConcurrentHashMap<>();

    private final CrateService crateService;
    private final CrateInventory crateInventory;
    private final MessagesConfig messages;
    private final Plugin plugin;

    /**
     * Crea el listener de crates con todas sus dependencias inyectadas por Guice.
     *
     * @param crateService   servicio para consultar las ubicaciones de crates persistidas en base de datos
     * @param crateInventory gestor de la interfaz gráfica de apertura de crates
     * @param messages       configuración de mensajes localizada
     * @param plugin         instancia del plugin principal, usada para el logger
     */
    @Inject
    public CrateListener(CrateService crateService, CrateInventory crateInventory,
                         MessagesConfig messages, Plugin plugin) {
        this.crateService   = crateService;
        this.crateInventory = crateInventory;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /**
     * Carga todas las ubicaciones de crates desde la base de datos y las almacena en la caché
     * en memoria, descartando los datos anteriores.
     *
     * <p>Este método se invoca durante el arranque del plugin ({@link dev.alexissdev.kronos.plugin.lifecycle.PluginEnableHandler#enable()})
     * y también puede llamarse manualmente para refrescar la caché tras cambios administrativos.
     */
    public void loadCrates() {
        crateService.getAllCrates().thenAccept(list -> {
            crateCache.clear();
            for (CrateLocation loc : list) {
                crateCache.put(locationKey(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()), loc.getType());
            }
            plugin.getLogger().info("Cargados " + list.size() + " crates en caché.");
        });
    }

    /**
     * Registra una nueva ubicación de crate en la caché en memoria.
     *
     * <p>Debe invocarse tras crear o mover un crate en la base de datos para mantener
     * la caché sincronizada sin necesidad de recargar todos los crates.
     *
     * @param loc objeto {@link CrateLocation} con el mundo, coordenadas y tipo del crate
     */
    public void registerCrate(CrateLocation loc) {
        crateCache.put(locationKey(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()), loc.getType());
    }

    /**
     * Elimina un crate de la caché en memoria por sus coordenadas.
     *
     * <p>Debe invocarse al eliminar un crate de la base de datos para que el bloque
     * correspondiente deje de tratarse como crate sin recargar toda la caché.
     *
     * @param world nombre del mundo donde se ubica el crate
     * @param x     coordenada X del bloque
     * @param y     coordenada Y del bloque
     * @param z     coordenada Z del bloque
     */
    public void unregisterCrate(String world, int x, int y, int z) {
        crateCache.remove(locationKey(world, x, y, z));
    }

    /**
     * Maneja el clic derecho de un jugador sobre un bloque que puede ser un crate.
     *
     * <p>Verifica que el bloque clickeado esté registrado en la caché de crates, cancela el
     * evento nativo de Bukkit para evitar interacciones secundarias, y comprueba que el jugador
     * tenga en la mano una llave del tipo correcto. Si la llave es válida, la consume (reduce
     * su cantidad en uno) y abre la animación de apertura del crate.
     *
     * @param event evento de interacción del jugador con un bloque
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        String key = locationKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        CrateType crateType = crateCache.get(key);
        if (crateType == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        ItemStack held = player.getItemInHand();
        CrateType keyType = getCrateType(held);

        if (keyType == null) {
            player.sendMessage(messages.format("crate.need-key", "type", crateType.name()));
            return;
        }

        if (keyType != crateType) {
            player.sendMessage(messages.format("crate.wrong-key",
                    "required", crateType.name(), "held", keyType.name()));
            return;
        }

        // Consume one key
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }

        crateInventory.openCrateAnimation(player, crateType);
    }

    /**
     * Crea un item de llave de crate para el tipo especificado.
     *
     * <p>La llave se representa como un gancho de trampilla ({@link Material#TRIPWIRE_HOOK}) con
     * nombre y lore personalizados. El lore contiene un prefijo especial ({@value #LORE_PREFIX})
     * seguido del nombre del tipo de crate, que se utiliza internamente para identificar el tipo
     * de llave sin depender del nombre visible.
     *
     * @param type tipo de crate para el que se genera la llave
     * @return un {@link ItemStack} configurado como llave del tipo indicado
     */
    public static ItemStack createKey(CrateType type) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Llave de Crate: " + ChatColor.YELLOW + type.name());
        meta.setLore(Arrays.asList(LORE_PREFIX + type.name()));
        item.setItemMeta(meta);
        return item;
    }

    private static CrateType getCrateType(ItemStack item) {
        if (item == null || item.getType() != Material.TRIPWIRE_HOOK) return null;
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith(LORE_PREFIX)) {
                try {
                    return CrateType.valueOf(line.substring(LORE_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    private static String locationKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
