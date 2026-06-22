package de.julian.buildplugin.npc;

import org.bukkit.Location;
import org.bukkit.World;

public class NPCData {

    private final String id;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final int buildTimeMinutes;
    private java.util.UUID entityUUID;

    public NPCData(String id, Location location, int buildTimeMinutes) {
        this.id = id;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.buildTimeMinutes = buildTimeMinutes;
    }

    public Location toLocation(org.bukkit.Server server) {
        World world = server.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, 0);
    }

    public String getId() { return id; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public int getBuildTimeMinutes() { return buildTimeMinutes; }
    public java.util.UUID getEntityUUID() { return entityUUID; }
    public void setEntityUUID(java.util.UUID uuid) { this.entityUUID = uuid; }
}
