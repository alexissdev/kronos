package dev.alexissdev.kronos.koth.creation;

import com.google.inject.Singleton;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Singleton
public class KothCreationService {

    private static final String WAND_NAME = ChatColor.GOLD + "" + ChatColor.BOLD + "KOTH Wand";

    private final Map<UUID, KothCreationSession> sessions = new ConcurrentHashMap<>();

    public void startSession(UUID uuid, String name, int captureTimeSeconds) {
        sessions.put(uuid, new KothCreationSession(name, captureTimeSeconds));
    }

    public Optional<KothCreationSession> getSession(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    public void cancelSession(UUID uuid) {
        sessions.remove(uuid);
    }

    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
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
