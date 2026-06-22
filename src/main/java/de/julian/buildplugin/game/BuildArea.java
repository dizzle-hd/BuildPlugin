package de.julian.buildplugin.game;

import org.bukkit.Location;
import org.bukkit.World;

public class BuildArea {

    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final World world;

    public BuildArea(World world, int minX, int maxX, int minZ, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(world)) return false;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public Location getCenter(int y) {
        int cx = (minX + maxX) / 2;
        int cz = (minZ + maxZ) / 2;
        return new Location(world, cx + 0.5, y, cz + 0.5);
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public World getWorld() { return world; }
}
