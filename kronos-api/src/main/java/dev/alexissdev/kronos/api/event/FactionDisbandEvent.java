package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class FactionDisbandEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String factionId;
    private final String factionName;
    private final UUID actorUuid;

    public FactionDisbandEvent(String factionId, String factionName, UUID actorUuid) {
        this.factionId = factionId;
        this.factionName = factionName;
        this.actorUuid = actorUuid;
    }

    public String getFactionId() { return factionId; }
    public String getFactionName() { return factionName; }
    public UUID getActorUuid() { return actorUuid; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
