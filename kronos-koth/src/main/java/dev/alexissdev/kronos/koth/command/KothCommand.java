package dev.alexissdev.kronos.koth.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.koth.creation.KothCreationService;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.service.KothService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Singleton
public class KothCommand extends BaseCommand {

    private static final String PREFIX = ChatColor.GOLD + "[KOTH] " + ChatColor.RESET;

    private final KothService kothService;
    private final KothCreationService creationService;
    private final Plugin plugin;

    @Inject
    public KothCommand(KothService kothService,
                       KothCreationService creationService,
                       Plugin plugin) {
        super("hcf.koth.admin");
        this.kothService     = kothService;
        this.creationService = creationService;
        this.plugin          = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "start":  handleStart(sender, args);  break;
            case "end":    handleEnd(sender, args);    break;
            case "list":   handleList(sender);         break;
            case "create": handleCreate(sender, args); break;
            case "delete": handleDelete(sender, args); break;
            default:       sendHelp(sender);
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth start <nombre>")) return;
        kothService.startKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> msg(sender, "&aKOTH &e" + args[1] + "&a iniciado!")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(sender, "&c" + rootMessage(ex)));
                    return null;
                });
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth end <nombre>")) return;
        kothService.endKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> msg(sender, "&eKOTH &f" + args[1] + "&e finalizado.")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(sender, "&c" + rootMessage(ex)));
                    return null;
                });
    }

    private void handleList(CommandSender sender) {
        kothService.getAllKoths().thenAccept(koths ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    msg(sender, "&6&lKOTHs registrados:");
                    if (koths.isEmpty()) {
                        msg(sender, "&7  (ninguno)");
                        return;
                    }
                    for (KothZone z : koths) {
                        String status = z.isActive() ? "&aACTIVO" : "&cINACTIVO";
                        msg(sender, "&e" + z.getName() + " &7- " + status
                                + " &7| Captura: &f" + z.getCaptureTimeSeconds() + "s");
                    }
                }));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "&cEste comando solo puede usarlo un jugador.");
            return;
        }
        if (!requireArgs(sender, args, 3, "/koth create <nombre> <tiempoCaptura(s)>")) return;

        Player player = (Player) sender;
        String name = args[1];
        int captureTime;
        try {
            captureTime = Integer.parseInt(args[2]);
            if (captureTime <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            msg(sender, "&cEl tiempo de captura debe ser un número positivo.");
            return;
        }

        creationService.startSession(player.getUniqueId(), name, captureTime);
        player.getInventory().addItem(creationService.createWand());

        player.sendMessage(PREFIX + ChatColor.WHITE + "Creando KOTH "
                + ChatColor.YELLOW + name
                + ChatColor.WHITE + " (captura: " + ChatColor.YELLOW + captureTime + "s" + ChatColor.WHITE + ")");
        player.sendMessage(ChatColor.GRAY + "Selecciona las 2 esquinas de la "
                + ChatColor.GREEN + "zona de claim" + ChatColor.GRAY + " con el wand:");
        player.sendMessage(ChatColor.GRAY + "  Clic izquierdo → " + ChatColor.GREEN + "Pos 1"
                + ChatColor.GRAY + "  │  Clic derecho → " + ChatColor.YELLOW + "Pos 2");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth delete <nombre>")) return;
        kothService.deleteKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> msg(sender, "&eKOTH &f" + args[1] + "&e eliminado.")));
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6&lComandos de KOTH:");
        msg(sender, "&e/koth create <nombre> <tiempoCaptura(s)> &7- Crear con wand");
        msg(sender, "&e/koth start <nombre> &7- Iniciar KOTH");
        msg(sender, "&e/koth end <nombre> &7- Finalizar KOTH");
        msg(sender, "&e/koth list &7- Listar KOTHs");
        msg(sender, "&e/koth delete <nombre> &7- Eliminar KOTH");
    }

    private static String rootMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
