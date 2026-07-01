package dev.alexissdev.kronos.api.event;

import dev.alexissdev.kronos.api.model.ClaimSnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FactionClaimEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String factionId;
    private final ClaimSnapshot claim;
    private boolean cancelled;

    public FactionClaimEvent(String factionId, ClaimSnapshot claim) {
        this.factionId = factionId;
        this.claim = claim;
    }

    public String getFactionId() { return factionId; }
    public ClaimSnapshot getClaim() { return claim; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
