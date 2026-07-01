package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlayerCombatTagEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID tagged;
    private final UUID tagger;
    private final long durationMillis;
    private boolean cancelled;

    public PlayerCombatTagEvent(UUID tagged, UUID tagger, long durationMillis) {
        this.tagged = tagged;
        this.tagger = tagger;
        this.durationMillis = durationMillis;
    }

    public UUID getTagged() { return tagged; }
    public UUID getTagger() { return tagger; }
    public long getDurationMillis() { return durationMillis; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
