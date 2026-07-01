package dev.alexissdev.kronos.koth.event;

import dev.alexissdev.kronos.koth.domain.KothZone;

public final class KothStartedDomainEvent {

    private final String kothName;
    private final int centerX;
    private final int centerZ;
    private final int captureTimeSeconds;
    private final KothZone zone;

    public KothStartedDomainEvent(String kothName, int centerX, int centerZ,
                                   int captureTimeSeconds, KothZone zone) {
        this.kothName            = kothName;
        this.centerX             = centerX;
        this.centerZ             = centerZ;
        this.captureTimeSeconds  = captureTimeSeconds;
        this.zone                = zone;
    }

    public String  getKothName()           { return kothName; }
    public int     getCenterX()            { return centerX; }
    public int     getCenterZ()            { return centerZ; }
    public int     getCaptureTimeSeconds() { return captureTimeSeconds; }
    public KothZone getZone()              { return zone; }
}
