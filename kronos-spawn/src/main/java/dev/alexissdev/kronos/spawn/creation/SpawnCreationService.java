package dev.alexissdev.kronos.spawn.creation;

import com.google.inject.Singleton;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SpawnCreationService {

    private static final String WAND_NAME = ChatColor.AQUA + "" + ChatColor.BOLD + "Spawn Wand";

    private final ConcurrentHashMap<UUID, SpawnCreationSession> sessions = new ConcurrentHashMap<>();

    public void startSession(UUID uuid) {
        sessions.put(uuid, new SpawnCreationSession());
    }

    public Optional<SpawnCreationSession> getSession(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    public void cancelSession(UUID uuid) {
        sessions.remove(uuid);
    }

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(WAND_NAME);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Clic izquierdo " + ChatColor.WHITE + "→ " + ChatColor.GREEN + "Posición 1",
                ChatColor.GRAY + "Clic derecho "   + ChatColor.WHITE + "→ " + ChatColor.YELLOW + "Posición 2"
        ));
        wand.setItemMeta(meta);
        return wand;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && WAND_NAME.equals(meta.getDisplayName());
    }
}
