package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlayerLeaveFactionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String factionId;
    private final boolean wasKicked;

    public PlayerLeaveFactionEvent(UUID playerUuid, String factionId, boolean wasKicked) {
        this.playerUuid = playerUuid;
        this.factionId = factionId;
        this.wasKicked = wasKicked;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getFactionId() { return factionId; }
    public boolean wasKicked() { return wasKicked; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
