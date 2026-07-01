package dev.alexissdev.kronos.presentation.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.domain.Claim;
import dev.alexissdev.kronos.core.domain.Faction;
import dev.alexissdev.kronos.core.exception.HCFException;
import dev.alexissdev.kronos.core.service.ClaimService;
import dev.alexissdev.kronos.core.service.FactionService;
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

    @Inject
    public FactionCommand(FactionService factionService, ClaimService claimService, Plugin plugin) {
        super(null);
        this.factionService = factionService;
        this.claimService = claimService;
        this.plugin = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

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
                        () -> msg(player, "&aFacción &e" + faction.getName() + "&a creada con éxito!")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
                    return null;
                });
    }

    private void handleDisband(Player player) {
        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.disbandFaction(opt.get().getId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> msg(player, "&aTu facción ha sido disuelta.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
                    return null;
                });
    }

    private void handleInvite(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f invite <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(player, "&cJugador no encontrado."); return; }

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.inviteMember(opt.get().getId(), target.getUniqueId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            msg(player, "&aInvitación enviada a &e" + target.getName());
            msg(target, "&e" + player.getName() + "&a te invitó a su facción. Usa &e/f accept <faccion>");
        })).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin,
                    () -> msg(player, "&c" + getRootCause(ex).getMessage()));
            return null;
        });
    }

    private void handleAccept(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f accept <faccion>")) return;
        factionService.getByName(args[1]).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("Facción no encontrada");
            return factionService.acceptInvite(player.getUniqueId(), opt.get().getId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> msg(player, "&aTe uniste a la facción!")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
                    return null;
                });
    }

    private void handleLeave(Player player) {
        factionService.leaveFaction(player.getUniqueId())
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> msg(player, "&aSaliste de tu facción.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
                    return null;
                });
    }

    private void handleKick(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f kick <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(player, "&cJugador no encontrado."); return; }

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.kickMember(opt.get().getId(), target.getUniqueId(), player.getUniqueId());
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> msg(player, "&aExpulsaste a &e" + target.getName())))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
                    return null;
                });
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length >= 2) {
            factionService.getByName(args[1]).thenAccept(opt ->
                    Bukkit.getScheduler().runTask(plugin,
                            () -> opt.ifPresentOrElse(f -> printFactionInfo(player, f),
                                    () -> msg(player, "&cFacción no encontrada."))));
        } else {
            factionService.getByPlayer(player.getUniqueId()).thenAccept(opt ->
                    Bukkit.getScheduler().runTask(plugin,
                            () -> opt.ifPresentOrElse(f -> printFactionInfo(player, f),
                                    () -> msg(player, "&cNo estás en ninguna facción."))));
        }
    }

    private void printFactionInfo(Player player, Faction f) {
        msg(player, "&7&m--------------------");
        msg(player, "&6Facción: &e" + f.getName());
        msg(player, "&6Miembros: &e" + f.getMembers().size());
        msg(player, "&6Kills: &e" + f.getKills() + " &6Deaths: &e" + f.getDeaths());
        msg(player, "&6DTK: &e" + f.getDtkRemaining() + "/" + f.getMaxDtk());
        msg(player, "&6Balance: &e$" + String.format("%.2f", f.getBalance()));
        msg(player, "&7&m--------------------");
    }

    private void handleChat(Player player) {
        msg(player, "&aChat de facción toggled. (WIP)");
    }

    private void handleTop(Player player) {
        factionService.getTopFactions(10).thenAccept(factions ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    msg(player, "&6&lTop Facciones:");
                    for (int i = 0; i < factions.size(); i++) {
                        Faction f = factions.get(i);
                        msg(player, "&e" + (i + 1) + ". &f" + f.getName() +
                                " &7- Kills: &e" + f.getKills());
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
                () -> msg(player, "&aAlianza establecida.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
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
                () -> msg(player, "&cRelación de enemigos establecida.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
                    return null;
                });
    }

    private void handleDeposit(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f deposit <cantidad>")) return;
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
            msg(player, "&cCantidad inválida."); return;
        }
        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.deposit(opt.get().getId(), player.getUniqueId(), amount);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> msg(player, "&aDepositaste &e$" + amount + "&a a la facción.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
                    return null;
                });
    }

    private void handleWithdraw(Player player, String[] args) {
        if (!requireArgs(player, args, 2, "/f withdraw <cantidad>")) return;
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) {
            msg(player, "&cCantidad inválida."); return;
        }
        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.withdraw(opt.get().getId(), player.getUniqueId(), amount);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> msg(player, "&aRetiraste &e$" + amount + "&a de la facción.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
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
                () -> msg(player, "&aReclamaste el chunk &e(" + cx + ", " + cz + ")&a.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
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
                () -> msg(player, "&aDesreclamaste el chunk &e(" + cx + ", " + cz + ")&a.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
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
                () -> msg(player, "&aOverclaim exitoso en chunk &e(" + cx + ", " + cz + ")&a.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(player, "&c" + getRootCause(ex).getMessage()));
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
            msg(player, "&7&m---------&r &6Mapa de Claims &7&m---------");
            lines.forEach(player::sendMessage);
            msg(player, "&a■&7=Propio &c■&7=Enemigo &e■&7=Aliado &9■&7=Sistema &7■&7=Libre");
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
        msg(player, "&6&lComandos de Facción:");
        msg(player, "&e/f create <nombre> &7- Crear facción");
        msg(player, "&e/f disband &7- Disolver facción");
        msg(player, "&e/f invite <jugador> &7- Invitar jugador");
        msg(player, "&e/f accept <faccion> &7- Aceptar invitación");
        msg(player, "&e/f leave &7- Salir de la facción");
        msg(player, "&e/f kick <jugador> &7- Expulsar miembro");
        msg(player, "&e/f info [faccion] &7- Ver información");
        msg(player, "&e/f top &7- Top facciones");
        msg(player, "&e/f ally <faccion> &7- Ser aliados");
        msg(player, "&e/f enemy <faccion> &7- Ser enemigos");
        msg(player, "&e/f deposit <$> &7- Depositar dinero");
        msg(player, "&e/f withdraw <$> &7- Retirar dinero");
        msg(player, "&e/f claim &7- Reclamar chunk actual");
        msg(player, "&e/f unclaim &7- Desreclamar chunk actual");
        msg(player, "&e/f overclaim &7- Overclaim territorio enemigo");
        msg(player, "&e/f map &7- Mapa de claims");
    }

    private Throwable getRootCause(Throwable ex) {
        return ex.getCause() != null ? ex.getCause() : ex;
    }
}
