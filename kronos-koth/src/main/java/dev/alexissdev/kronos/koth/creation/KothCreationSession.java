package dev.alexissdev.kronos.koth.creation;

import dev.alexissdev.kronos.common.domain.CrateType;
import dev.alexissdev.kronos.koth.domain.KothZone;

public final class KothCreationSession {

    public enum Phase { CLAIM, CAPTURE }

    private final String kothName;
    private final int captureTimeSeconds;
    private Phase phase = Phase.CLAIM;

    private String worldName;

    private Integer claimX1, claimZ1;
    private Integer claimX2, claimZ2;
    private Integer capX1, capZ1;
    private Integer capX2, capZ2;

    public KothCreationSession(String kothName, int captureTimeSeconds) {
        this.kothName = kothName;
        this.captureTimeSeconds = captureTimeSeconds;
    }

    // ── setters ───────────────────────────────────────────────────────────

    public void setClaimPos1(String world, int x, int z) {
        this.worldName = world;
        this.claimX1 = x;
        this.claimZ1 = z;
    }

    public void setClaimPos2(int x, int z) {
        this.claimX2 = x;
        this.claimZ2 = z;
    }

    public void setCapturePos1(int x, int z) {
        this.capX1 = x;
        this.capZ1 = z;
    }

    public void setCapturePos2(int x, int z) {
        this.capX2 = x;
        this.capZ2 = z;
    }

    public void advanceToCapture() {
        this.phase = Phase.CAPTURE;
    }

    // ── state checks ──────────────────────────────────────────────────────

    public boolean hasClaimPos1()   { return claimX1 != null; }
    public boolean isClaimComplete() { return claimX1 != null && claimX2 != null; }
    public boolean hasCapturePos1() { return capX1 != null; }
    public boolean isComplete()     { return isClaimComplete() && capX1 != null && capX2 != null; }

    // ── getters ───────────────────────────────────────────────────────────

    public Phase getPhase()          { return phase; }
    public String getKothName()      { return kothName; }
    public int getCaptureTimeSeconds() { return captureTimeSeconds; }

    // ── build ─────────────────────────────────────────────────────────────

    public KothZone build() {
        return new KothZone(
                kothName, worldName,
                Math.min(claimX1, claimX2), Math.min(claimZ1, claimZ2),
                Math.max(claimX1, claimX2), Math.max(claimZ1, claimZ2),
                Math.min(capX1, capX2), Math.min(capZ1, capZ2),
                Math.max(capX1, capX2), Math.max(capZ1, capZ2),
                captureTimeSeconds, CrateType.KOTH
        );
    }
}
