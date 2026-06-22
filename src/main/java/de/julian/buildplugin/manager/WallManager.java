package de.julian.buildplugin.manager;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WallManager {

    private final Plugin plugin;
    private final int wallHeight;

    private static final int BASE_Y = AreaManager.BEDROCK_Y;

    public WallManager(Plugin plugin, int wallHeight) {
        this.plugin = plugin;
        this.wallHeight = wallHeight;
    }

    /**
     * Places BARRIER blocks around the outer perimeter of the arena.
     * These stay in place for the entire game (build phase + voting phase).
     * Only removed when the game is fully stopped.
     *
     * Perimeter sits 1 block outside the plots:
     *   X = centerX ± (areaSize + 1)
     *   Z = centerZ ± (areaSize + 1)
     */
    public void placeOuterBarriers(World world, int centerX, int centerZ, int areaSize) {
        int minX = centerX - areaSize - 1;
        int maxX = centerX + areaSize + 1;
        int minZ = centerZ - areaSize - 1;
        int maxZ = centerZ + areaSize + 1;

        scheduleFill(world, () -> {
            for (int y = BASE_Y; y < BASE_Y + wallHeight; y++) {
                for (int x = minX; x <= maxX; x++) {
                    place(world, x, y, minZ, Material.BARRIER);
                    place(world, x, y, maxZ, Material.BARRIER);
                }
                for (int z = minZ + 1; z < maxZ; z++) {
                    place(world, minX, y, z, Material.BARRIER);
                    place(world, maxX, y, z, Material.BARRIER);
                }
            }
        });
    }

    /**
     * Removes the outer BARRIER perimeter. Called only on game stop/reset.
     */
    public void removeOuterBarriers(World world, int centerX, int centerZ, int areaSize) {
        int minX = centerX - areaSize - 1;
        int maxX = centerX + areaSize + 1;
        int minZ = centerZ - areaSize - 1;
        int maxZ = centerZ + areaSize + 1;

        scheduleFill(world, () -> {
            for (int y = BASE_Y; y < BASE_Y + wallHeight; y++) {
                for (int x = minX; x <= maxX; x++) {
                    clearBarrier(world, x, y, minZ);
                    clearBarrier(world, x, y, maxZ);
                }
                for (int z = minZ + 1; z < maxZ; z++) {
                    clearBarrier(world, minX, y, z);
                    clearBarrier(world, maxX, y, z);
                }
            }
        });
    }

    /**
     * Places RED_CONCRETE in the 1-block "+" divider between the 4 plots.
     * Removed at the start of the voting phase so players can see all builds.
     *
     * "+" shape:
     *   - Column at X = centerX  (from Z = centerZ-areaSize to centerZ+areaSize)
     *   - Row    at Z = centerZ  (from X = centerX-areaSize to centerX+areaSize)
     */
    public void placeRedDivider(World world, int centerX, int centerZ, int areaSize) {
        scheduleFill(world, () -> {
            for (int y = BASE_Y; y < BASE_Y + wallHeight; y++) {
                // Vertical bar (runs along Z at X=center)
                for (int z = centerZ - areaSize; z <= centerZ + areaSize; z++) {
                    place(world, centerX, y, z, Material.RED_CONCRETE);
                }
                // Horizontal bar (runs along X at Z=center)
                for (int x = centerX - areaSize; x <= centerX + areaSize; x++) {
                    place(world, x, y, centerZ, Material.RED_CONCRETE);
                }
            }
        });
    }

    /**
     * Removes the red "+" divider at the start of the voting phase.
     */
    public void removeRedDivider(World world, int centerX, int centerZ, int areaSize) {
        scheduleFill(world, () -> {
            for (int y = BASE_Y; y < BASE_Y + wallHeight; y++) {
                for (int z = centerZ - areaSize; z <= centerZ + areaSize; z++) {
                    clearMaterial(world, centerX, y, z, Material.RED_CONCRETE);
                }
                for (int x = centerX - areaSize; x <= centerX + areaSize; x++) {
                    clearMaterial(world, x, y, centerZ, Material.RED_CONCRETE);
                }
            }
        });
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void scheduleFill(World world, Runnable blockOps) {
        new BukkitRunnable() {
            @Override
            public void run() {
                blockOps.run();
            }
        }.runTask(plugin);
    }

    private void place(World world, int x, int y, int z, Material mat) {
        world.getBlockAt(x, y, z).setType(mat, false);
    }

    private void clearBarrier(World world, int x, int y, int z) {
        if (world.getBlockAt(x, y, z).getType() == Material.BARRIER) {
            world.getBlockAt(x, y, z).setType(Material.AIR, false);
        }
    }

    private void clearMaterial(World world, int x, int y, int z, Material mat) {
        if (world.getBlockAt(x, y, z).getType() == mat) {
            world.getBlockAt(x, y, z).setType(Material.AIR, false);
        }
    }
}
