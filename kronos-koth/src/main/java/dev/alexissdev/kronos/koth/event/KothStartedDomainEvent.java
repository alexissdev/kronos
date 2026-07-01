package dev.alexissdev.kronos.koth.event;

public final class KothStartedDomainEvent {

    private final String kothName;
    private final int centerX;
    private final int centerZ;
    private final int captureTimeSeconds;

    public KothStartedDomainEvent(String kothName, int centerX, int centerZ, int captureTimeSeconds) {
        this.kothName = kothName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.captureTimeSeconds = captureTimeSeconds;
    }

    public String getKothName()        { return kothName; }
    public int getCenterX()            { return centerX; }
    public int getCenterZ()            { return centerZ; }
    public int getCaptureTimeSeconds() { return captureTimeSeconds; }
}
