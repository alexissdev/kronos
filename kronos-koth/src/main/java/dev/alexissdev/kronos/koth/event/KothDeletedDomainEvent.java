package dev.alexissdev.kronos.koth.event;

public final class KothDeletedDomainEvent {

    private final String kothName;

    public KothDeletedDomainEvent(String kothName) {
        this.kothName = kothName;
    }

    public String getKothName() { return kothName; }
}
