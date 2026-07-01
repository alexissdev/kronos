package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.KothSnapshot;

import java.util.List;
import java.util.Optional;

/** KOTH state queries for external plugins. */
public interface KothApi {

    List<KothSnapshot> getActiveKoths();

    Optional<KothSnapshot> getKoth(String name);

    boolean isKothActive(String name);
}
