package dev.alexissdev.kronos.plugin.command;

import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.claims.domain.Claim;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.claims.service.ClaimService;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class FactionCommand extends BaseCommand {

    private static final int MAP_RADIUS_X = 4;
    private static final int MAP_RADIUS_Z = 3;

    private final FactionService factionService;
    private final ClaimService claimService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    @Inject
    public FactionCommand(FactionService factionService, ClaimService claimService,
                          Plugin plugin, MessagesConfig messages) {
        super(null);
        this.factionService = factionService;
        this.claimService = claimService;
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length == 0) { sendHelp(player); return; }

        switch (args[0].toLowerCase()) {
            case "create": handleCreate(player, args); break;
            case "disband": handleDisband(player); break;
            case "invite": handleInvite(player, args); break;
            case "accept": handleAccept(player, args); break;
            case "leave": handleLeave(player); break;
            case "kick": handleKick(player, args); break;
            case "info": handleInfo(player, args); break;
            case "chat": handleChat(player); break;
            case "top": handleTop(player); break;
            case "ally": handleAlly(player, args); break;
            case "enemy": handleEnemy(player, args); break;
            case "deposit": handleDeposit(player, args); break;
            case "withdraw": handleWithdraw(player, args); break;
            case "map": handleMap(player); break;
            case "claim": handleClaim(player); break;
            case "unclaim": handleUnclaim(player); break;
            case "overclaim": handleOverclaim(player); break;
            default: sendHelp(player);
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f create <nombre>")) return;
        factionService.createFaction(args[1], player.getUniqueId())
                .thenAccept(faction -> Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.created", "name", faction.getName()))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleDisband(Player player) {
        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.disbandFaction(opt.get().getId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.disbanded"))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleInvite(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f invite <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(messages.get("faction.cmd.player-not-found")); return; }

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.inviteMember(opt.get().getId(), target.getUniqueId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(messages.format("faction.cmd.invite-sent", "player", target.getName()));
            target.sendMessage(messages.format("faction.cmd.invite-received", "player", player.getName()));
        })).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin,
                    () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
            return null;
        });
    }

    private void handleAccept(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f accept <faccion>")) return;
        factionService.getByName(args[1]).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("Facción no encontrada");
            return factionService.acceptInvite(player.getUniqueId(), opt.get().getId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.joined"))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleLeave(Player player) {
        factionService.leaveFaction(player.getUniqueId())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.get("faction.cmd.left"))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleKick(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f kick <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(messages.get("faction.cmd.player-not-found")); return; }

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.kickMember(opt.get().getId(), target.getUniqueId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.kicked", "player", target.getName()))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length >= 2) {
            factionService.getByName(args[1]).thenAccept(opt ->
                    Bukkit.getScheduler().runTask(plugin,
                            () -> opt.ifPresentOrElse(f -> printFactionInfo(player, f),
                                    () -> player.sendMessage(messages.get("faction.cmd.faction-not-found")))));
        } else {
            factionService.getByPlayer(player.getUniqueId()).thenAccept(opt ->
                    Bukkit.getScheduler().runTask(plugin,
                            () -> opt.ifPresentOrElse(f -> printFactionInfo(player, f),
                                    () -> player.sendMessage(messages.get("faction.cmd.not-in-faction")))));
        }
    }

    private void printFactionInfo(Player player, Faction f) {
        player.sendMessage(messages.get("faction.cmd.info-sep"));
        player.sendMessage(messages.format("faction.cmd.info-name", "name", f.getName()));
        player.sendMessage(messages.format("faction.cmd.info-members", "count", f.getMembers().size()));
        player.sendMessage(messages.format("faction.cmd.info-stats", "kills", f.getKills(), "deaths", f.getDeaths()));
        player.sendMessage(messages.format("faction.cmd.info-dtk", "remaining", f.getDtkRemaining(), "max", f.getMaxDtk()));
        player.sendMessage(messages.format("faction.cmd.info-balance", "balance", String.format("%.2f", f.getBalance())));
        player.sendMessage(messages.get("faction.cmd.info-sep"));
    }

    private void handleChat(Player player) {
        player.sendMessage(messages.get("faction.cmd.chat-toggled"));
    }

    private void handleTop(Player player) {
        factionService.getTopFactions(10).thenAccept(factions ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(messages.get("faction.cmd.top-header"));
                    for (int i = 0; i < factions.size(); i++) {
                        Faction f = factions.get(i);
                        player.sendMessage(messages.format("faction.cmd.top-entry",
                                "rank", i + 1, "name", f.getName(), "kills", f.getKills()));
                    }
                }));
    }

    private void handleAlly(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f ally <faccion>")) return;
        factionService.getByPlayer(player.getUniqueId()).thenCompose(optA -> {
            if (optA.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.getByName(args[1]).thenCompose(optB -> {
                if (optB.isEmpty()) throw new HCFException("Facción no encontrada");
                return factionService.setAlly(optA.get().getId(), optB.get().getId(), player.getUniqueId());
            });
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.ally-set"))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleEnemy(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f enemy <faccion>")) return;
        factionService.getByPlayer(player.getUniqueId()).thenCompose(optA -> {
            if (optA.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.getByName(args[1]).thenCompose(optB -> {
                if (optB.isEmpty()) throw new HCFException("Facción no encontrada");
                return factionService.setEnemy(optA.get().getId(), optB.get().getId(), player.getUniqueId());
            });
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.get("faction.cmd.enemy-set"))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleDeposit(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f deposit <cantidad>")) return;
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
            player.sendMessage(messages.get("faction.cmd.amount-invalid")); return;
        }
        final double finalAmount = amount;
        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.deposit(opt.get().getId(), player.getUniqueId(), finalAmount);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.deposited",
                        "amount", String.format("%.2f", finalAmount)))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleWithdraw(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f withdraw <cantidad>")) return;
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
            player.sendMessage(messages.get("faction.cmd.amount-invalid")); return;
        }
        final double finalAmount = amount;
        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.withdraw(opt.get().getId(), player.getUniqueId(), finalAmount);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.withdrawn",
                        "amount", String.format("%.2f", finalAmount)))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleClaim(Player player) {
        int cx = player.getLocation().getChunk().getX();
        int cz = player.getLocation().getChunk().getZ();
        String world = player.getWorld().getName();

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return claimService.claim(opt.get().getId(), player.getUniqueId(), world, cx, cz, cx, cz);
        }).thenAccept(claim -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.claimed", "x", cx, "z", cz))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleUnclaim(Player player) {
        int cx = player.getLocation().getChunk().getX();
        int cz = player.getLocation().getChunk().getZ();
        String world = player.getWorld().getName();

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return claimService.unclaim(opt.get().getId(), player.getUniqueId(), world, cx, cz);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.unclaimed", "x", cx, "z", cz))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleOverclaim(Player player) {
        int cx = player.getLocation().getChunk().getX();
        int cz = player.getLocation().getChunk().getZ();
        String world = player.getWorld().getName();

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return claimService.overclaim(opt.get().getId(), player.getUniqueId(), world, cx, cz);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.overclaimed", "x", cx, "z", cz))))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex))));
                    return null;
                });
    }

    private void handleMap(Player player) {
        int centerX = player.getLocation().getChunk().getX();
        int centerZ = player.getLocation().getChunk().getZ();
        String world = player.getWorld().getName();

        int totalChunks = (MAP_RADIUS_X * 2 + 1) * (MAP_RADIUS_Z * 2 + 1);
        @SuppressWarnings("unchecked")
        CompletableFuture<Optional<Claim>>[] futures = new CompletableFuture[totalChunks];

        int i = 0;
        for (int dz = -MAP_RADIUS_Z; dz <= MAP_RADIUS_Z; dz++) {
            for (int dx = -MAP_RADIUS_X; dx <= MAP_RADIUS_X; dx++) {
                futures[i++] = claimService.getClaimAt(world, centerX + dx, centerZ + dz);
            }
        }

        factionService.getByPlayer(player.getUniqueId()).thenCombine(
                CompletableFuture.allOf(futures).thenApply(v -> {
                    @SuppressWarnings("unchecked")
                    Optional<Claim>[] results = new Optional[totalChunks];
                    for (int j = 0; j < totalChunks; j++) results[j] = futures[j].join();
                    return results;
                }),
                (playerFaction, claimGrid) -> {
                    Set<String> allyIds = playerFaction.map(Faction::getAllies).orElse(Collections.emptySet());
                    Set<String> enemyIds = playerFaction.map(Faction::getEnemies).orElse(Collections.emptySet());
                    String ownId = playerFaction.map(Faction::getId).orElse(null);
                    return buildMapLines(claimGrid, ownId, allyIds, enemyIds, centerX, centerZ);
                }
        ).thenAccept(lines -> Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(messages.get("faction.cmd.map-header"));
            lines.forEach(player::sendMessage);
            player.sendMessage(messages.get("faction.cmd.map-legend"));
        }));
    }

    private List<String> buildMapLines(Optional<Claim>[] grid, String ownId,
                                       Set<String> allyIds, Set<String> enemyIds,
                                       int centerX, int centerZ) {
        int width = MAP_RADIUS_X * 2 + 1;
        int height = MAP_RADIUS_Z * 2 + 1;
        List<String> lines = new ArrayList<>();

        for (int row = 0; row < height; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < width; col++) {
                Optional<Claim> opt = grid[row * width + col];
                boolean isCenter = (col == MAP_RADIUS_X && row == MAP_RADIUS_Z);

                if (isCenter) {
                    sb.append(ChatColor.WHITE).append("[").append(ChatColor.YELLOW).append("+").append(ChatColor.WHITE).append("]");
                } else if (opt.isEmpty()) {
                    sb.append(ChatColor.GRAY).append(" ■ ");
                } else {
                    Claim c = opt.get();
                    switch (c.getType()) {
                        case SAFEZONE: case WARZONE: case ROAD:
                            sb.append(ChatColor.DARK_AQUA).append(" ■ "); break;
                        case KOTH: case CITADEL:
                            sb.append(ChatColor.LIGHT_PURPLE).append(" ■ "); break;
                        default:
                            if (c.getFactionId().equals(ownId))           sb.append(ChatColor.GREEN).append(" ■ ");
                            else if (allyIds.contains(c.getFactionId()))   sb.append(ChatColor.YELLOW).append(" ■ ");
                            else if (enemyIds.contains(c.getFactionId()))  sb.append(ChatColor.RED).append(" ■ ");
                            else                                            sb.append(ChatColor.DARK_RED).append(" ■ ");
                    }
                }
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    private void sendHelp(Player player) {
        player.sendMessage(messages.get("faction.cmd.help-header"));
        player.sendMessage(messages.get("faction.cmd.help-create"));
        player.sendMessage(messages.get("faction.cmd.help-disband"));
        player.sendMessage(messages.get("faction.cmd.help-invite"));
        player.sendMessage(messages.get("faction.cmd.help-accept"));
        player.sendMessage(messages.get("faction.cmd.help-leave"));
        player.sendMessage(messages.get("faction.cmd.help-kick"));
        player.sendMessage(messages.get("faction.cmd.help-info"));
        player.sendMessage(messages.get("faction.cmd.help-top"));
        player.sendMessage(messages.get("faction.cmd.help-ally"));
        player.sendMessage(messages.get("faction.cmd.help-enemy"));
        player.sendMessage(messages.get("faction.cmd.help-deposit"));
        player.sendMessage(messages.get("faction.cmd.help-withdraw"));
        player.sendMessage(messages.get("faction.cmd.help-claim"));
        player.sendMessage(messages.get("faction.cmd.help-unclaim"));
        player.sendMessage(messages.get("faction.cmd.help-overclaim"));
        player.sendMessage(messages.get("faction.cmd.help-map"));
    }

    private static String rootMsg(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
