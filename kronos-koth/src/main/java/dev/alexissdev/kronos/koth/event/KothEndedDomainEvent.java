package dev.alexissdev.kronos.koth.event;

public final class KothEndedDomainEvent {

    private final String kothName;

    public KothEndedDomainEvent(String kothName) {
        this.kothName = kothName;
    }

    public String getKothName() { return kothName; }
}
