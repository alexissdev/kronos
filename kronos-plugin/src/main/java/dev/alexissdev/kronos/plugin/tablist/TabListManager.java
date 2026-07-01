package dev.alexissdev.kronos.plugin.tablist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import dev.alexissdev.kronos.factions.domain.FactionRole;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@Singleton
public class TabListManager {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin plugin;

    @Inject
    public TabListManager(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages = messages;
        this.plugin = plugin;
    }

    public void refresh(Player player) {
        factionService.getByPlayer(player.getUniqueId()).thenAccept(opt -> {
            String displayName;
            if (opt.isPresent()) {
                Faction faction = opt.get();
                FactionMember member = faction.getMember(player.getUniqueId()).orElse(null);
                FactionRole role = member != null ? member.getRole() : FactionRole.MEMBER;
                String key = "tablist." + role.name().toLowerCase();
                displayName = messages.format(key, "faction", faction.getName(), "player", player.getName());
            } else {
                displayName = messages.format("tablist.no-faction", "player", player.getName());
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) player.setPlayerListName(displayName);
            });
        });
    }

    public void refresh(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) refresh(player);
    }

    public void refreshAll() {
        Bukkit.getOnlinePlayers().forEach(this::refresh);
    }
}
