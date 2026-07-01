package dev.alexissdev.kronos.spawn.creation;

import dev.alexissdev.kronos.spawn.domain.SpawnZone;

public final class SpawnCreationSession {

    private String worldName;
    private Integer x1, z1;
    private Integer x2, z2;

    public void setPos1(String world, int x, int z) {
        this.worldName = world;
        this.x1 = x;
        this.z1 = z;
    }

    public void setPos2(int x, int z) {
        this.x2 = x;
        this.z2 = z;
    }

    public boolean hasPos1()    { return x1 != null; }
    public boolean isComplete() { return x1 != null && x2 != null; }

    public SpawnZone build() {
        return new SpawnZone(
                worldName,
                Math.min(x1, x2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(z1, z2)
        );
    }
}
