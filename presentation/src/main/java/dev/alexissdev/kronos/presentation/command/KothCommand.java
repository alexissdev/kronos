package dev.alexissdev.kronos.presentation.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.domain.CrateType;
import dev.alexissdev.kronos.core.domain.KothZone;
import dev.alexissdev.kronos.core.service.KotHService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

@Singleton
public class KothCommand extends BaseCommand {

    private final KotHService kothService;
    private final Plugin plugin;

    @Inject
    public KothCommand(KotHService kothService, Plugin plugin) {
        super("hcf.koth.admin");
        this.kothService = kothService;
        this.plugin = plugin;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sendHelp(sender); return; }

        switch (args[0].toLowerCase()) {
            case "start": handleStart(sender, args); break;
            case "end": handleEnd(sender, args); break;
            case "list": handleList(sender); break;
            case "create": handleCreate(sender, args); break;
            case "delete": handleDelete(sender, args); break;
            default: sendHelp(sender);
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth start <nombre>")) return;
        kothService.startKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> msg(sender, "&aKOTH &e" + args[1] + "&a iniciado!")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> msg(sender, "&c" + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
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
                            () -> msg(sender, "&c" + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
                    return null;
                });
    }

    private void handleList(CommandSender sender) {
        kothService.getAllKoths().thenAccept(koths ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    msg(sender, "&6&lKOTHs registrados:");
                    for (KothZone z : koths) {
                        String status = z.isActive() ? "&aACTIVO" : "&cINACTIVO";
                        msg(sender, "&e" + z.getName() + " &7- " + status);
                    }
                }));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 7, "/koth create <nombre> <mundo> <minX> <minZ> <maxX> <maxZ>")) return;
        try {
            String name = args[1];
            String world = args[2];
            int minX = Integer.parseInt(args[3]);
            int minZ = Integer.parseInt(args[4]);
            int maxX = Integer.parseInt(args[5]);
            int maxZ = Integer.parseInt(args[6]);

            KothZone zone = new KothZone(name, world, minX, minZ, maxX, maxZ, 300, CrateType.KOTH);
            kothService.createKoth(zone)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                            () -> msg(sender, "&aKOTH &e" + name + "&a creado con éxito!")));
        } catch (NumberFormatException e) {
            msg(sender, "&cCoordenadas inválidas.");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth delete <nombre>")) return;
        kothService.deleteKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> msg(sender, "&eKOTH &f" + args[1] + "&e eliminado.")));
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6&lComandos de KOTH:");
        msg(sender, "&e/koth start <nombre> &7- Iniciar KOTH");
        msg(sender, "&e/koth end <nombre> &7- Finalizar KOTH");
        msg(sender, "&e/koth list &7- Listar KOTHs");
        msg(sender, "&e/koth create <nombre> <mundo> <minX> <minZ> <maxX> <maxZ>");
        msg(sender, "&e/koth delete <nombre> &7- Eliminar KOTH");
    }
}
