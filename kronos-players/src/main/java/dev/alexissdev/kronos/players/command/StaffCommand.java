package dev.alexissdev.kronos.players.command;

import dev.alexissdev.kronos.common.command.BaseCommand;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.players.service.StaffService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

@Singleton
public class StaffCommand extends BaseCommand {

    private final StaffService staffService;
    private final Plugin plugin;

    @Inject
    public StaffCommand(StaffService staffService, Plugin plugin) {
        super("hcf.staff");
        this.staffService = staffService;
        this.plugin = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length == 0) { toggleStaffMode(player); return; }

        switch (args[0].toLowerCase()) {
            case "vanish": toggleVanish(player); break;
            case "freeze": handleFreeze(player, args); break;
            case "unfreeze": handleUnfreeze(player, args); break;
            default: toggleStaffMode(player);
        }
    }

    private void toggleStaffMode(Player player) {
        staffService.isInStaffMode(player.getUniqueId()).thenCompose(inMode -> {
            if (inMode) {
                return staffService.disableStaffMode(player.getUniqueId())
                        .thenRun(() -> Bukkit.getScheduler().runTask(
                                plugin,
                                () -> {
                                    player.setAllowFlight(false);
                                    player.setFlying(false);
                                    msg(player, "&cModo staff &lDESACTIVADO&c.");
                                }));
            } else {
                return staffService.enableStaffMode(player.getUniqueId())
                        .thenRun(() -> Bukkit.getScheduler().runTask(
                                plugin,
                                () -> {
                                    player.setAllowFlight(true);
                                    msg(player, "&aModo staff &lACTIVADO&a.");
                                }));
            }
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> msg(player, "&c" + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
            return null;
        });
    }

    private void toggleVanish(Player player) {
        boolean currentlyVanished = player.hasPotionEffect(PotionEffectType.INVISIBILITY);

        staffService.isInStaffMode(player.getUniqueId()).thenCompose(inMode -> {
            if (!inMode) {
                Bukkit.getScheduler().runTask(plugin,
                        () -> msg(player, "&cDebes estar en modo staff para usar vanish."));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            return staffService.setVanish(player.getUniqueId(), !currentlyVanished)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!currentlyVanished) {
                            Bukkit.getOnlinePlayers().forEach(p -> {
                                if (!p.hasPermission("hcf.staff")) p.hidePlayer(plugin, player);
                            });
                            msg(player, "&7Ahora eres invisible.");
                        } else {
                            Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(plugin, player));
                            msg(player, "&7Ya no eres invisible.");
                        }
                    }));
        });
    }

    private void handleFreeze(Player staff, String[] args) {
        if (!requireArgs(staff, args, 2, "/staff freeze <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(staff, "&cJugador no encontrado."); return; }

        staffService.freeze(staff.getUniqueId(), target.getUniqueId())
                .thenRun(() -> Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {
                            msg(staff, "&aCongelaste a &e" + target.getName());
                            msg(target, "&c¡Fuiste congelado por un staff member!");
                        }));
    }

    private void handleUnfreeze(Player staff, String[] args) {
        if (!requireArgs(staff, args, 2, "/staff unfreeze <jugador>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(staff, "&cJugador no encontrado."); return; }

        staffService.unfreeze(target.getUniqueId())
                .thenRun(() -> Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {
                            msg(staff, "&aDescongelaste a &e" + target.getName());
                            msg(target, "&aFuiste descongelado.");
                        }));
    }
}
