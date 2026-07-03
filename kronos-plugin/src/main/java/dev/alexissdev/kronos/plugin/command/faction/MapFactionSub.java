package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Singleton
public class MapFactionSub extends FactionSubCommand {

    private static final int RADIUS_X = 4;
    private static final int RADIUS_Z = 3;

    private final FactionService factionService;
    private final ClaimService   claimService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public MapFactionSub(FactionService factionService, ClaimService claimService,
                         MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.claimService   = claimService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    @Override public String name() { return "map"; }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        int centerX = player.getLocation().getChunk().getX();
        int centerZ = player.getLocation().getChunk().getZ();
        String world = player.getWorld().getName();
        int total = (RADIUS_X * 2 + 1) * (RADIUS_Z * 2 + 1);

        CompletableFuture<Optional<Claim>>[] futures = new CompletableFuture[total];
        int i = 0;
        for (int dz = -RADIUS_Z; dz <= RADIUS_Z; dz++)
            for (int dx = -RADIUS_X; dx <= RADIUS_X; dx++)
                futures[i++] = claimService.getClaimAt(world, centerX + dx, centerZ + dz);

        factionService.getByPlayer(player.getUniqueId()).thenCombine(
                CompletableFuture.allOf(futures).thenApply(v -> {
                    Optional<Claim>[] grid = new Optional[total];
                    for (int j = 0; j < total; j++) grid[j] = futures[j].join();
                    return grid;
                }),
                (playerFaction, grid) -> {
                    Set<String> allyIds  = playerFaction.map(Faction::getAllies).orElse(Collections.emptySet());
                    Set<String> enemyIds = playerFaction.map(Faction::getEnemies).orElse(Collections.emptySet());
                    String ownId = playerFaction.map(Faction::getId).orElse(null);
                    return buildLines(grid, ownId, allyIds, enemyIds);
                }
        ).thenAccept(lines -> Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(messages.get("faction.cmd.map-header"));
            lines.forEach(player::sendMessage);
            player.sendMessage(messages.get("faction.cmd.map-legend"));
        }));
    }

    private List<String> buildLines(Optional<Claim>[] grid, String ownId,
                                    Set<String> allyIds, Set<String> enemyIds) {
        int width  = RADIUS_X * 2 + 1;
        int height = RADIUS_Z * 2 + 1;
        List<String> lines = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < width; col++) {
                Optional<Claim> opt = grid[row * width + col];
                boolean isCenter = (col == RADIUS_X && row == RADIUS_Z);
                if (isCenter) {
                    sb.append(ChatColor.WHITE).append("[").append(ChatColor.YELLOW).append("+").append(ChatColor.WHITE).append("]");
                } else if (opt.isEmpty()) {
                    sb.append(ChatColor.GRAY).append(" ■ ");
                } else {
                    Claim c = opt.get();
                    if (c.getType() == ClaimType.SAFEZONE || c.getType() == ClaimType.WARZONE || c.getType() == ClaimType.ROAD) {
                        sb.append(ChatColor.DARK_AQUA).append(" ■ ");
                    } else if (c.getType() == ClaimType.KOTH || c.getType() == ClaimType.CITADEL) {
                        sb.append(ChatColor.LIGHT_PURPLE).append(" ■ ");
                    } else if (c.getFactionId().equals(ownId))          sb.append(ChatColor.GREEN).append(" ■ ");
                    else if (allyIds.contains(c.getFactionId()))         sb.append(ChatColor.YELLOW).append(" ■ ");
                    else if (enemyIds.contains(c.getFactionId()))        sb.append(ChatColor.RED).append(" ■ ");
                    else                                                  sb.append(ChatColor.DARK_RED).append(" ■ ");
                }
            }
            lines.add(sb.toString());
        }
        return lines;
    }
}
