package de.julian.buildplugin.manager;

import de.julian.buildplugin.game.BuildArea;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class WallManager {

    private final Plugin plugin;
    private final int wallHeight;

    public WallManager(Plugin plugin, int wallHeight) {
        this.plugin = plugin;
        this.wallHeight = wallHeight;
    }

    public void placeOuterWalls(World world, int centerX, int centerZ, int totalSize) {
        int half = totalSize / 2;
        int minX = centerX - half;
        int maxX = centerX + half;
        int minZ = centerZ - half;
        int maxZ = centerZ + half;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int y = 0; y < wallHeight; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        setBlock(world, x, y, minZ, Material.BARRIER);
                        setBlock(world, x, y, maxZ, Material.BARRIER);
                    }
                    for (int z = minZ; z <= maxZ; z++) {
                        setBlock(world, minX, y, z, Material.BARRIER);
                        setBlock(world, maxX, y, z, Material.BARRIER);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void removeOuterWalls(World world, int centerX, int centerZ, int totalSize) {
        int half = totalSize / 2;
        int minX = centerX - half;
        int maxX = centerX + half;
        int minZ = centerZ - half;
        int maxZ = centerZ + half;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int y = 0; y < wallHeight; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        removeBarrier(world, x, y, minZ);
                        removeBarrier(world, x, y, maxZ);
                    }
                    for (int z = minZ; z <= maxZ; z++) {
                        removeBarrier(world, minX, y, z);
                        removeBarrier(world, maxX, y, z);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void placeInnerWalls(World world, int centerX, int centerZ, int totalSize) {
        int half = totalSize / 2;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int y = 0; y < wallHeight; y++) {
                    // Vertical divider (along Z axis at centerX)
                    for (int z = centerZ - half; z <= centerZ + half; z++) {
                        setBlock(world, centerX, y, z, Material.BARRIER);
                    }
                    // Horizontal divider (along X axis at centerZ)
                    for (int x = centerX - half; x <= centerX + half; x++) {
                        setBlock(world, x, y, centerZ, Material.BARRIER);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void removeInnerWalls(World world, int centerX, int centerZ, int totalSize) {
        int half = totalSize / 2;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int y = 0; y < wallHeight; y++) {
                    for (int z = centerZ - half; z <= centerZ + half; z++) {
                        removeBarrier(world, centerX, y, z);
                    }
                    for (int x = centerX - half; x <= centerX + half; x++) {
                        removeBarrier(world, x, y, centerZ);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        new BukkitRunnable() {
            @Override
            public void run() {
                world.getBlockAt(x, y, z).setType(material);
            }
        }.runTask(plugin);
    }

    private void removeBarrier(World world, int x, int y, int z) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (world.getBlockAt(x, y, z).getType() == Material.BARRIER) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }.runTask(plugin);
    }
}
