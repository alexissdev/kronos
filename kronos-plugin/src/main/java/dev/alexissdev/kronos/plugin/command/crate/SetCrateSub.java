package dev.alexissdev.kronos.plugin.command.crate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.players.service.CrateService;
import dev.alexissdev.kronos.plugin.listener.CrateListener;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sub-comando {@code /crate set <tipo>} que registra el bloque al que está
 * mirando el ejecutor como un cofre de recompensas del tipo especificado.
 * Persiste la ubicación en la base de datos y la registra en el listener activo
 * para que el cofre comience a procesarse inmediatamente.
 */
@Singleton
public class SetCrateSub extends SubCommand {

    private final CrateService   crateService;
    private final CrateListener  crateListener;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param crateService  servicio para persistir la nueva ubicación del cofre
     * @param crateListener listener de cofres, usado para registrar el cofre en tiempo real
     * @param messages      configuración de mensajes localizados
     * @param plugin        instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public SetCrateSub(CrateService crateService, CrateListener crateListener,
                       MessagesConfig messages, Plugin plugin) {
        this.crateService  = crateService;
        this.crateListener = crateListener;
        this.messages      = messages;
        this.plugin        = plugin;
    }

    /** @return el nombre del sub-comando: {@code "set"} */
    @Override public String name() { return "set"; }

    /**
     * Proporciona sugerencias de autocompletado con los valores del enum
     * {@link dev.alexissdev.kronos.common.domain.CrateType} para el argumento de tipo.
     *
     * @param sender ejecutor del comando
     * @param args   argumentos escritos hasta el momento
     * @return lista de tipos de crate que coinciden con el prefijo del segundo argumento
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2
                ? filterPrefix(Arrays.stream(CrateType.values()).map(CrateType::name).collect(Collectors.toList()), args[1])
                : List.of();
    }

    /**
     * Valida el tipo de crate proporcionado, obtiene el bloque en la mira del ejecutor
     * y lo persiste como cofre de recompensas del tipo indicado. En caso de éxito
     * registra el cofre en el listener activo y notifica al ejecutor con las coordenadas.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos; {@code args[0]} es el literal {@code "set"},
     *               {@code args[1]} es el nombre del tipo de crate
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/crate set <tipo>")) return;

        CrateType type;
        try { type = CrateType.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException e) {
            String valid = Arrays.stream(CrateType.values()).map(CrateType::name).collect(Collectors.joining(", "));
            player.sendMessage(messages.format("crate.cmd.invalid-type", "valid", valid)); return;
        }

        Block target = player.getTargetBlock((HashSet<Byte>) null, 5);
        if (target == null) { player.sendMessage(messages.get("crate.cmd.no-target")); return; }

        CrateType finalType = type;
        crateService.setCrate(target.getWorld().getName(), target.getX(), target.getY(), target.getZ(), type)
                .thenAccept(loc -> {
                    crateListener.registerCrate(loc);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage(messages.format("crate.cmd.set",
                                    "type", finalType.name(),
                                    "x", String.valueOf(target.getX()),
                                    "y", String.valueOf(target.getY()),
                                    "z", String.valueOf(target.getZ()))));
                }).exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin, () ->
                player.sendMessage(messages.format("crate.cmd.error", "error", ex.getMessage()))); return null; });
    }
}
