package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Sub-comando {@code /hcf give-money <jugador> <cantidad>} que deposita una
 * cantidad de dinero en el balance del jugador indicado de forma asíncrona.
 * Notifica tanto al ejecutor como al destinatario del saldo añadido.
 */
@Singleton
public class GiveMoneySub extends SubCommand {

    private final EconomyService economyService;
    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param economyService servicio de economía para realizar el depósito
     * @param messages       configuración de mensajes localizados
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     */
    @Inject
    public GiveMoneySub(EconomyService economyService, MessagesConfig messages, Plugin plugin) {
        this.economyService = economyService;
        this.messages       = messages;
        this.plugin         = plugin;
    }

    /** @return el nombre del sub-comando: {@code "give-money"} */
    @Override public String name() { return "give-money"; }

    /**
     * Proporciona sugerencias de autocompletado con los nombres de los jugadores
     * en línea para el segundo argumento.
     *
     * @param sender ejecutor del comando
     * @param args   argumentos escritos hasta el momento
     * @return lista de jugadores en línea que coinciden con el prefijo del segundo argumento
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 2 ? onlinePlayers(args[1]) : List.of();
    }

    /**
     * Valida los argumentos, parsea la cantidad económica y deposita el importe
     * en el balance del jugador objetivo de forma asíncrona. El resultado y las
     * notificaciones se entregan en el hilo principal de Bukkit.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos; {@code args[1]} es el nombre del jugador objetivo,
     *               {@code args[2]} es la cantidad numérica a depositar
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/hcf give-money <jugador> <cantidad>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("hcf.player-not-found")); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); }
        catch (NumberFormatException e) { sender.sendMessage(messages.get("hcf.amount-invalid")); return; }
        final double finalAmount = amount;

        economyService.deposit(target.getUniqueId(), finalAmount)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.format("hcf.give-money-sender",
                            "amount", String.format("%.2f", finalAmount), "player", target.getName()));
                    target.sendMessage(messages.format("hcf.give-money-target",
                            "amount", String.format("%.2f", finalAmount)));
                }));
    }
}
