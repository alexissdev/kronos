package dev.alexissdev.kronos.common.domain;

public interface SotwService {

    void startSotw(long durationMs);
    void stopSotw();
    boolean isSotwActive();
    long getSotwRemainingMs();

    void startEotw(long durationMs);
    void stopEotw();
    boolean isEotwActive();
    long getEotwRemainingMs();
}
