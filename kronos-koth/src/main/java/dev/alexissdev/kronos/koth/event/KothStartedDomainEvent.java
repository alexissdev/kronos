package dev.alexissdev.kronos.koth.event;

public final class KothStartedDomainEvent {

    private final String kothName;

    public KothStartedDomainEvent(String kothName) {
        this.kothName = kothName;
    }

    public String getKothName() { return kothName; }
}
