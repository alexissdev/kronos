package dev.alexissdev.kronos.economy.command;

import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Comando {@code /money} del plugin Kronos HCF que expone las operaciones de economía a los jugadores.
 *
 * <p>Gestiona las siguientes suboperaciones:</p>
 * <ul>
 *   <li><strong>Sin argumentos</strong>: muestra el saldo del jugador que ejecuta el comando.</li>
 *   <li><strong>{@code <jugador>}</strong>: muestra el saldo de otro jugador online.</li>
 *   <li><strong>{@code pay <jugador> <cantidad>}</strong>: transfiere dinero a otro jugador conectado.</li>
 *   <li><strong>{@code top}</strong>: muestra el ranking de los 10 jugadores online con mayor saldo.</li>
 * </ul>
 *
 * <p>Todas las operaciones de economía son asíncronas (devuelven {@link CompletableFuture})
 * y los mensajes de respuesta se envían al jugador de vuelta en el hilo principal de Bukkit
 * usando el scheduler del plugin.</p>
 *
 * <p>No requiere permiso especial (pasa {@code null} a {@link BaseCommand}), por lo que
 * cualquier jugador puede usarlo. Se registra como {@code @Singleton} para reutilizar
 * la instancia inyectada por Guice.</p>
 */
@Singleton
public class MoneyCommand extends BaseCommand {

    private final EconomyService economyService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    /**
     * Construye el comando de dinero con sus dependencias inyectadas por Guice.
     *
     * @param economyService servicio de economía que ejecuta las operaciones financieras
     * @param plugin         instancia del plugin principal para programar tareas en el hilo principal
     * @param messages       configuración de mensajes para obtener los textos localizados
     */
    @Inject
    public MoneyCommand(EconomyService economyService, Plugin plugin, MessagesConfig messages) {
        super(null);
        this.economyService = economyService;
        this.plugin = plugin;
        this.messages = messages;
    }

    /**
     * Genera sugerencias de autocompletado para el comando {@code /money}.
     * <ul>
     *   <li>Primer argumento: sugiere {@code pay} y {@code top}.</li>
     *   <li>Segundo argumento tras {@code pay}: sugiere nombres de jugadores online.</li>
     * </ul>
     *
     * @param sender quien está escribiendo el comando
     * @param args   argumentos parciales introducidos hasta el momento
     * @return lista de sugerencias contextuales
     */
    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return subcommands(args, "pay", "top");
        if (args.length == 2 && args[0].equalsIgnoreCase("pay")) return onlinePlayers(args[1]);
        return Collections.emptyList();
    }

    /**
     * Punto de entrada principal del comando {@code /money}. Solo puede ser ejecutado por jugadores.
     * Analiza los argumentos y delega a los manejadores específicos según la acción solicitada.
     *
     * @param sender quien ejecutó el comando; debe ser un jugador
     * @param args   argumentos del comando: vacío para ver propio saldo, {@code pay} para pagar,
     *               {@code top} para el ranking, o un nombre de jugador para ver su saldo
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;

        if (args.length == 0) { showBalance(player, player.getUniqueId(), player.getName()); return; }

        switch (args[0].toLowerCase()) {
            case "pay": handlePay(player, args); break;
            case "top": handleTop(player); break;
            default:
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) showBalance(player, target.getUniqueId(), target.getName());
                else player.sendMessage(messages.get("economy.player-not-found"));
        }
    }

    /**
     * Consulta el saldo del jugador indicado y envía el resultado al observador.
     * El mensaje varía dependiendo de si el observador está consultando su propio saldo
     * o el de otro jugador.
     *
     * @param viewer      jugador que recibirá el mensaje con el saldo
     * @param targetUuid  UUID del jugador cuyo saldo se consulta
     * @param targetName  nombre del jugador cuyo saldo se consulta, para incluirlo en el mensaje
     */
    private void showBalance(Player viewer, UUID targetUuid, String targetName) {
        economyService.getBalance(targetUuid)
                .thenAccept(balance -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (viewer.getUniqueId().equals(targetUuid)) {
                        viewer.sendMessage(messages.format("economy.balance-self",
                                "amount", String.format("%.2f", balance)));
                    } else {
                        viewer.sendMessage(messages.format("economy.balance-other",
                                "player", targetName,
                                "amount", String.format("%.2f", balance)));
                    }
                }));
    }

    /**
     * Maneja la suboperación {@code /money pay <jugador> <cantidad>}.
     * Valida todos los casos de error (jugador no encontrado, autoenvío, cantidad inválida o
     * no positiva) antes de ejecutar la transferencia. Si la transferencia falla (p. ej.
     * fondos insuficientes), informa al remitente con el mensaje de error correspondiente.
     *
     * @param sender jugador que inicia el pago
     * @param args   argumentos del comando; {@code args[1]} es el destinatario y {@code args[2]} la cantidad
     */
    private void handlePay(Player sender, String[] args) {
        if (!requireArgs(sender, args, 3, "/money pay <jugador> <cantidad>")) return;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(messages.get("economy.player-not-found")); return; }
        if (target.equals(sender)) { sender.sendMessage(messages.get("economy.pay-self")); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("economy.amount-invalid")); return;
        }
        if (amount <= 0) { sender.sendMessage(messages.get("economy.amount-positive")); return; }

        final double finalAmount = amount;
        economyService.transfer(sender.getUniqueId(), target.getUniqueId(), finalAmount)
                .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(messages.format("economy.pay-sent",
                            "amount", String.format("%.2f", finalAmount),
                            "player", target.getName()));
                    target.sendMessage(messages.format("economy.pay-received",
                            "sender", sender.getName(),
                            "amount", String.format("%.2f", finalAmount)));
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(messages.format("economy.error",
                                    "error", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
                    return null;
                });
    }

    /**
     * Maneja la suboperación {@code /money top}, mostrando los 10 jugadores online
     * con mayor saldo ordenados de mayor a menor.
     *
     * <p>Consulta el saldo de todos los jugadores conectados en paralelo usando
     * {@link CompletableFuture}s independientes y espera a que todas completen antes
     * de ordenar y mostrar el resultado en el hilo principal de Bukkit.</p>
     *
     * @param player jugador que solicita ver el ranking de riqueza
     */
    private void handleTop(Player player) {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) { player.sendMessage(messages.get("economy.player-not-found")); return; }

        List<CompletableFuture<Map.Entry<String, Double>>> futures = online.stream()
                .map(p -> economyService.getBalance(p.getUniqueId())
                        .thenApply(bal -> (Map.Entry<String, Double>) new AbstractMap.SimpleEntry<>(p.getName(), bal)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    List<Map.Entry<String, Double>> sorted = futures.stream()
                            .map(CompletableFuture::join)
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .limit(10)
                            .collect(Collectors.toList());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(messages.get("economy.top-header"));
                        for (int i = 0; i < sorted.size(); i++) {
                            player.sendMessage(messages.format("economy.top-entry",
                                    "rank", i + 1,
                                    "player", sorted.get(i).getKey(),
                                    "amount", String.format("%.2f", sorted.get(i).getValue())));
                        }
                    });
                });
    }
}
