package dev.alexissdev.kronos.plugin.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.config.MessagesConfig;
import dev.alexissdev.kronos.players.repository.DeathbanRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.OptionalLong;
import java.util.UUID;

@Singleton
public class DeathbanListener implements Listener {

    private final DeathbanRepository deathbanRepository;
    private final MessagesConfig messages;

    @Inject
    public DeathbanListener(DeathbanRepository deathbanRepository, MessagesConfig messages) {
        this.deathbanRepository = deathbanRepository;
        this.messages = messages;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        OptionalLong remaining = deathbanRepository.getRemainingSeconds(uuid).join();
        if (remaining.isPresent()) {
            String formatted = formatDuration(remaining.getAsLong());
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    messages.format("deathban.banned-screen", "time", formatted));
        }
    }

    private static String formatDuration(long totalSeconds) {
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0)   return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
