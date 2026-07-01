package dev.alexissdev.kronos.scoreboard;

final class KothEntry {

    final String name;
    final int centerX;
    final int centerZ;
    final int captureTimeSeconds;

    KothEntry(String name, int centerX, int centerZ, int captureTimeSeconds) {
        this.name = name;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.captureTimeSeconds = captureTimeSeconds;
    }
}
