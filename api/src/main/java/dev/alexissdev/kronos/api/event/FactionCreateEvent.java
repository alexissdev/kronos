package dev.alexissdev.kronos.api.event;

import dev.alexissdev.kronos.api.model.FactionSnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FactionCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FactionSnapshot faction;
    private boolean cancelled;

    public FactionCreateEvent(FactionSnapshot faction) {
        this.faction = faction;
    }

    public FactionSnapshot getFaction() { return faction; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
