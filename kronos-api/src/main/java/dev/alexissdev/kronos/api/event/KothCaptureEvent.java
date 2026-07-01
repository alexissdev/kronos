package dev.alexissdev.kronos.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class KothCaptureEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String kothName;
    private final UUID captorUuid;

    public KothCaptureEvent(String kothName, UUID captorUuid) {
        this.kothName = kothName;
        this.captorUuid = captorUuid;
    }

    public String getKothName() { return kothName; }
    public UUID getCaptorUuid() { return captorUuid; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
