package dev.alexissdev.kronos.plugin.sotw;

import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.domain.SotwService;

@Singleton
public class SotwManager implements SotwService {

    private volatile long sotwEndMs = 0L;
    private volatile long eotwEndMs = 0L;

    @Override
    public void startSotw(long durationMs) {
        sotwEndMs = System.currentTimeMillis() + durationMs;
    }

    @Override
    public void stopSotw() {
        sotwEndMs = 0L;
    }

    @Override
    public boolean isSotwActive() {
        return sotwEndMs > 0 && System.currentTimeMillis() < sotwEndMs;
    }

    @Override
    public long getSotwRemainingMs() {
        if (!isSotwActive()) return 0L;
        return sotwEndMs - System.currentTimeMillis();
    }

    @Override
    public void startEotw(long durationMs) {
        eotwEndMs = System.currentTimeMillis() + durationMs;
    }

    @Override
    public void stopEotw() {
        eotwEndMs = 0L;
    }

    @Override
    public boolean isEotwActive() {
        return eotwEndMs > 0 && System.currentTimeMillis() < eotwEndMs;
    }

    @Override
    public long getEotwRemainingMs() {
        if (!isEotwActive()) return 0L;
        return eotwEndMs - System.currentTimeMillis();
    }
}
