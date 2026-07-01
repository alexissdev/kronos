package dev.alexissdev.kronos.plugin.chat;

import com.google.inject.Singleton;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Singleton
public class ChatManager {

    private final Map<UUID, ChatMode> modes = new ConcurrentHashMap<>();

    public ChatMode getMode(UUID uuid) {
        return modes.getOrDefault(uuid, ChatMode.GLOBAL);
    }

    /** Cycles GLOBAL → FACTION → ALLY → GLOBAL and returns the new mode. */
    public ChatMode cycleMode(UUID uuid) {
        ChatMode next;
        switch (getMode(uuid)) {
            case GLOBAL:  next = ChatMode.FACTION; break;
            case FACTION: next = ChatMode.ALLY;    break;
            default:      next = ChatMode.GLOBAL;
        }
        if (next == ChatMode.GLOBAL) {
            modes.remove(uuid);
        } else {
            modes.put(uuid, next);
        }
        return next;
    }

    public void reset(UUID uuid) {
        modes.remove(uuid);
    }
}
