package dev.alexissdev.kronos.api.facade;

import dev.alexissdev.kronos.api.model.ClaimSnapshot;
import dev.alexissdev.kronos.claims.domain.ClaimType;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

/** Claim/territory queries for external plugins. */
public interface ClaimApi {

    Optional<ClaimSnapshot> getClaimAt(World world, int chunkX, int chunkZ);

    ClaimType getClaimTypeAt(Location location);

    boolean isClaimed(World world, int chunkX, int chunkZ);

    boolean isWilderness(Location location);

    boolean isSafeZone(Location location);

    boolean isWarZone(Location location);
}
