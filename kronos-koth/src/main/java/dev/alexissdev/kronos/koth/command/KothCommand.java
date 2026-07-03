package dev.alexissdev.kronos.koth.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.koth.creation.KothCreationService;
import dev.alexissdev.kronos.koth.domain.KothZone;
import dev.alexissdev.kronos.koth.service.KothService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Comando administrativo {@code /koth} para la gestión de eventos KOTH en el servidor HCF.
 *
 * <p>Permite a los administradores (permiso {@code hcf.koth.admin}) realizar las siguientes
 * operaciones sobre las zonas KOTH registradas:
 * <ul>
 *   <li>{@code /koth start <nombre>} — inicia un evento KOTH existente.</li>
 *   <li>{@code /koth end <nombre>} — finaliza un evento KOTH activo sin captura.</li>
 *   <li>{@code /koth list} — muestra la lista de todos los KOTHs con su estado.</li>
 *   <li>{@code /koth create <nombre> <tiempoCaptura(s)>} — inicia una sesión de creación
 *       interactiva y entrega la varita de selección al administrador.</li>
 *   <li>{@code /koth delete <nombre>} — elimina permanentemente un KOTH.</li>
 * </ul>
 *
 * <p>Todas las operaciones de estado son asíncronas; la retroalimentación al emisor del
 * comando se envía de vuelta en el hilo principal de Bukkit mediante el scheduler.</p>
 */
@Singleton
public class KothCommand extends BaseCommand {

    private final KothService kothService;
    private final KothCreationService creationService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    /**
     * Constructor inyectado por Guice con todas las dependencias del comando.
     *
     * @param kothService     servicio de negocio para operar sobre zonas KOTH
     * @param creationService servicio que gestiona las sesiones de creación interactiva
     * @param plugin          instancia del plugin de Bukkit, usada para el scheduler
     * @param messages        configuración de mensajes del plugin para internacionalización
     */
    @Inject
    public KothCommand(KothService kothService,
                       KothCreationService creationService,
                       Plugin plugin,
                       MessagesConfig messages) {
        super("hcf.koth.admin");
        this.kothService     = kothService;
        this.creationService = creationService;
        this.plugin          = plugin;
        this.messages        = messages;
    }

    /**
     * Provee sugerencias de autocompletado para el comando {@code /koth}.
     * En el primer argumento sugiere los subcomandos disponibles; en el segundo argumento,
     * para {@code start}, {@code end} y {@code delete}, sugiere los nombres de los KOTHs
     * registrados filtrados por el prefijo ya escrito.
     *
     * @param sender remitente del comando que solicita el autocompletado
     * @param args   argumentos parciales ya escritos por el remitente
     * @return lista de sugerencias de autocompletado según el contexto actual
     */
    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return subcommands(args, "start", "end", "list", "create", "delete");
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "start": case "end": case "delete":
                    return kothService.getAllKoths().thenApply(list -> {
                        return filterPrefix(
                                list.stream().map(k -> k.getName()).collect(java.util.stream.Collectors.toList()),
                                args[1]);
                    }).getNow(java.util.Collections.emptyList());
            }
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Despacha la ejecución del comando {@code /koth} al sub-manejador correspondiente
     * según el primer argumento recibido. Si no se proveen argumentos o el subcomando
     * no es reconocido, muestra el menú de ayuda.
     *
     * @param sender remitente del comando (jugador u consola)
     * @param args   argumentos del comando; {@code args[0]} determina el subcomando
     */
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
        String name = args[1];
        kothService.startKoth(name)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.format("koth.cmd.started", "name", name))))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msgKey = cause instanceof IllegalStateException
                            ? "koth.cmd.already-active"
                            : cause instanceof dev.alexissdev.kronos.koth.exception.KothNotFoundException
                            ? "koth.cmd.not-found"
                            : "koth.cmd.error";
                    String msg = msgKey.equals("koth.cmd.error")
                            ? messages.format(msgKey, "error", cause.getMessage())
                            : messages.format(msgKey, "name", name);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
                    return null;
                });
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth end <nombre>")) return;
        String name = args[1];
        kothService.endKoth(name)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.format("koth.cmd.ended", "name", name))))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msgKey = cause instanceof IllegalStateException
                            ? "koth.cmd.not-active"
                            : cause instanceof dev.alexissdev.kronos.koth.exception.KothNotFoundException
                            ? "koth.cmd.not-found"
                            : "koth.cmd.error";
                    String msg = msgKey.equals("koth.cmd.error")
                            ? messages.format(msgKey, "error", cause.getMessage())
                            : messages.format(msgKey, "name", name);
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(msg));
                    return null;
                });
    }

    private void handleList(CommandSender sender) {
        kothService.getAllKoths().thenAccept(koths ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.get("koth.cmd.list-header"));
                    if (koths.isEmpty()) {
                        sender.sendMessage(messages.get("koth.cmd.list-none"));
                        return;
                    }
                    for (KothZone z : koths) {
                        String status = z.isActive()
                                ? messages.format("koth.cmd.list-entry",
                                        "name", z.getName(),
                                        "status", "§aACTIVO",
                                        "seconds", z.getCaptureTimeSeconds())
                                : messages.format("koth.cmd.list-entry",
                                        "name", z.getName(),
                                        "status", "§cINACTIVO",
                                        "seconds", z.getCaptureTimeSeconds());
                        sender.sendMessage(status);
                    }
                }));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("koth.cmd.player-only"));
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
            sender.sendMessage(messages.get("koth.cmd.capture-time-invalid"));
            return;
        }

        creationService.startSession(player.getUniqueId(), name, captureTime);
        player.getInventory().addItem(creationService.createWand());

        player.sendMessage(messages.format("koth.cmd.creating", "name", name, "seconds", captureTime));
        player.sendMessage(messages.get("koth.cmd.select-claim-hint"));
        player.sendMessage(messages.get("koth.cmd.select-controls"));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2, "/koth delete <nombre>")) return;
        kothService.deleteKoth(args[1])
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                        () -> sender.sendMessage(messages.format("koth.cmd.deleted", "name", args[1]))));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("koth.cmd.help-header"));
        sender.sendMessage(messages.get("koth.cmd.help-create"));
        sender.sendMessage(messages.get("koth.cmd.help-start"));
        sender.sendMessage(messages.get("koth.cmd.help-end"));
        sender.sendMessage(messages.get("koth.cmd.help-list"));
        sender.sendMessage(messages.get("koth.cmd.help-delete"));
    }

    private static String rootMsg(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
