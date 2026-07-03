package dev.alexissdev.kronos.plugin.command.hcf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.command.SubCommand;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sub-comando {@code /hcf reload} que recarga en caliente el archivo
 * {@code messages.yml} del plugin sin necesidad de reiniciar el servidor.
 * Lee el archivo YAML desde el disco y actualiza el {@link MessagesConfig}
 * en memoria con los nuevos valores.
 */
@Singleton
public class ReloadSub extends SubCommand {

    private final MessagesConfig messages;
    private final Plugin         plugin;

    /**
     * Construye el sub-comando inyectando sus dependencias mediante Guice.
     *
     * @param messages configuración de mensajes que será recargada
     * @param plugin   instancia del plugin, usada para localizar la carpeta de datos
     */
    @Inject
    public ReloadSub(MessagesConfig messages, Plugin plugin) {
        this.messages = messages;
        this.plugin   = plugin;
    }

    /** @return el nombre del sub-comando: {@code "reload"} */
    @Override public String name() { return "reload"; }

    /**
     * Comprueba que el archivo {@code messages.yml} existe en la carpeta de datos
     * del plugin, lo carga como {@link org.bukkit.configuration.file.YamlConfiguration},
     * extrae todos los pares clave-valor no nulos y actualiza el {@link MessagesConfig}
     * en memoria. Notifica al ejecutor si la recarga fue exitosa o si el archivo no existe.
     *
     * @param sender ejecutor del comando (jugador o consola)
     * @param args   argumentos adicionales (no utilizados por este sub-comando)
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) { sender.sendMessage(messages.get("hcf.reload-file-not-found")); return; }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) map.put(key, yaml.getString(key, ""));
        }
        messages.reload(map);
        sender.sendMessage(messages.get("hcf.reloaded"));
    }
}
