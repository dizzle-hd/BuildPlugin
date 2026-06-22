package de.julian.buildplugin.queue;

import java.util.UUID;

public class QueueEntry {

    private final UUID playerUUID;
    private final boolean solo;
    private final int buildTimeMinutes;

    public QueueEntry(UUID playerUUID, boolean solo, int buildTimeMinutes) {
        this.playerUUID = playerUUID;
        this.solo = solo;
        this.buildTimeMinutes = buildTimeMinutes;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public boolean isSolo() { return solo; }
    public int getBuildTimeMinutes() { return buildTimeMinutes; }

    public String getKey() {
        return (solo ? "solo" : "team") + "_" + buildTimeMinutes;
    }
}
