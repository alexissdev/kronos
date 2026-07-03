package dev.alexissdev.kronos.plugin.command.faction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.common.exception.HCFException;
import dev.alexissdev.kronos.factions.service.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Sub-comando {@code /f deposit <cantidad>} que transfiere dinero del balance
 * personal del jugador ejecutor al banco colectivo de su facción. El saldo de
 * la facción se usa para el mantenimiento del territorio y otras mecánicas HCF.
 */
@Singleton
public class DepositFactionSub extends FactionSubCommand {

    private final FactionService factionService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param factionService servicio de facciones para realizar el depósito en el banco de la facción
     * @param messages       configuración de mensajes localizados
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public DepositFactionSub(FactionService factionService, MessagesConfig messages, Plugin plugin) {
        this.factionService = factionService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return el nombre del sub-comando: {@code "deposit"} */
    @Override public String name() { return "deposit"; }

    /**
     * Valida la cantidad a depositar, obtiene la facción del ejecutor y transfiere
     * el importe al banco de la facción de forma asíncrona. Notifica al jugador
     * en caso de éxito o error.
     *
     * @param sender ejecutor del comando; debe ser un {@link org.bukkit.entity.Player}
     * @param args   argumentos; {@code args[1]} es la cantidad numérica a depositar
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!requireArgs(player, args, 2, "/f deposit <cantidad>")) return;

        double amount;
        try { amount = Double.parseDouble(args[1]); }
        catch (NumberFormatException e) { player.sendMessage(messages.get("faction.cmd.amount-invalid")); return; }
        final double finalAmount = amount;

        factionService.getByPlayer(player.getUniqueId()).thenCompose(opt -> {
            if (opt.isEmpty()) throw new HCFException("No estás en una facción");
            return factionService.deposit(opt.get().getId(), player.getUniqueId(), finalAmount);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin,
                () -> player.sendMessage(messages.format("faction.cmd.deposited", "amount", String.format("%.2f", finalAmount)))))
                .exceptionally(ex -> { Bukkit.getScheduler().runTask(plugin,
                        () -> player.sendMessage(messages.format("faction.cmd.error", "error", rootMsg(ex)))); return null; });
    }
}
