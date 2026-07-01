package dev.alexissdev.kronos.core.event;

import java.util.UUID;

public final class KothCapturedDomainEvent {

    private final String kothName;
    private final UUID captorUuid;

    public KothCapturedDomainEvent(String kothName, UUID captorUuid) {
        this.kothName = kothName;
        this.captorUuid = captorUuid;
    }

    public String getKothName() { return kothName; }

    public UUID getCaptorUuid() { return captorUuid; }
}
