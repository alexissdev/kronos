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

@Singleton
public class ReloadSub extends SubCommand {

    private final MessagesConfig messages;
    private final Plugin         plugin;

    @Inject
    public ReloadSub(MessagesConfig messages, Plugin plugin) {
        this.messages = messages;
        this.plugin   = plugin;
    }

    @Override public String name() { return "reload"; }

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
