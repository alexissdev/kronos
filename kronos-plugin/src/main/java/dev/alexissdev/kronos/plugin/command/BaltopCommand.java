package dev.alexissdev.kronos.plugin.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.BaseCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Comando {@code /baltop} que muestra el ranking de los 10 jugadores en línea
 * con mayor balance económico. Las consultas de saldo se realizan de forma
 * asíncrona usando {@link java.util.concurrent.CompletableFuture} y el resultado
 * se entrega en el hilo principal de Bukkit para garantizar la seguridad del hilo.
 */
@Singleton
public class BaltopCommand extends BaseCommand {

    private final EconomyService economyService;
    private final Plugin plugin;
    private final MessagesConfig messages;

    /**
     * Construye el comando inyectando sus dependencias mediante Guice.
     *
     * @param economyService servicio de economía para consultar balances de jugadores
     * @param plugin         instancia del plugin, usada para programar tareas en el hilo principal
     * @param messages       configuración de mensajes localizados
     */
    @Inject
    public BaltopCommand(EconomyService economyService, Plugin plugin, MessagesConfig messages) {
        super(null);
        this.economyService = economyService;
        this.plugin         = plugin;
        this.messages       = messages;
    }

    /**
     * Consulta el saldo de todos los jugadores en línea de forma asíncrona,
     * los ordena de mayor a menor y muestra al ejecutor el top 10.
     * Si no hay jugadores en línea, envía el mensaje de error correspondiente.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos adicionales (no utilizados por este comando)
     */
    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            sender.sendMessage(messages.get("economy.player-not-found"));
            return;
        }

        List<CompletableFuture<Map.Entry<String, Double>>> futures = Bukkit.getOnlinePlayers().stream()
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
                        sender.sendMessage(messages.get("economy.top-header"));
                        for (int i = 0; i < sorted.size(); i++) {
                            sender.sendMessage(messages.format("economy.top-entry",
                                    "rank",   i + 1,
                                    "player", sorted.get(i).getKey(),
                                    "amount", String.format("%.2f", sorted.get(i).getValue())));
                        }
                    });
                });
    }
}
