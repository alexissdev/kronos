package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlayerJoinFactionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String factionId;
    private boolean cancelled;

    public PlayerJoinFactionEvent(UUID playerUuid, String factionId) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getFactionId() { return factionId; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
