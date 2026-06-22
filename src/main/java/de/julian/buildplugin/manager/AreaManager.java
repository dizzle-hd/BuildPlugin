package de.julian.buildplugin.manager;

import de.julian.buildplugin.game.BuildArea;
import de.julian.buildplugin.game.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AreaManager {

    // Platform Y levels in the void world
    public static final int PLATFORM_Y = 64;       // grass surface
    public static final int BEDROCK_Y = 63;        // bedrock floor
    public static final int SPAWN_Y = PLATFORM_Y + 1;

    private final Plugin plugin;

    public AreaManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Calculates the 4 build areas in a 2x2 grid around (centerX, centerZ).
     * Layout (each area is areaSize x areaSize):
     *   [Team1][Team2]
     *   [Team3][Team4]
     * A 1-block gap at center serves as the inner wall column.
     */
    public List<BuildArea> createGrid(World world, int centerX, int centerZ, int areaSize) {
        List<BuildArea> areas = new ArrayList<>();

        // 1-block gap at X=centerX and Z=centerZ holds the red divider wall.
        // Each plot is exactly areaSize × areaSize blocks.
        // Team 1: top-left
        areas.add(new BuildArea(world, centerX - areaSize, centerX - 1, centerZ - areaSize, centerZ - 1));
        // Team 2: top-right
        areas.add(new BuildArea(world, centerX + 1, centerX + areaSize, centerZ - areaSize, centerZ - 1));
        // Team 3: bottom-left
        areas.add(new BuildArea(world, centerX - areaSize, centerX - 1, centerZ + 1, centerZ + areaSize));
        // Team 4: bottom-right
        areas.add(new BuildArea(world, centerX + 1, centerX + areaSize, centerZ + 1, centerZ + areaSize));

        return areas;
    }

    /**
     * Generates a flat bedrock + grass platform for a build area in the void world.
     * Runs async in chunks to avoid lag.
     */
    public void generatePlatform(BuildArea area) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = area.getWorld();
                for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
                    for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
                        final int fx = x, fz = z;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                world.getBlockAt(fx, BEDROCK_Y, fz).setType(Material.BEDROCK);
                                world.getBlockAt(fx, PLATFORM_Y, fz).setType(Material.GRASS_BLOCK);
                            }
                        }.runTask(plugin);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Clears all blocks above the platform (for arena reset between games).
     */
    public void clearPlatform(BuildArea area, int maxHeight) {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = area.getWorld();
                for (int x = area.getMinX(); x <= area.getMaxX(); x++) {
                    for (int z = area.getMinZ(); z <= area.getMaxZ(); z++) {
                        final int fx = x, fz = z;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                for (int y = SPAWN_Y; y <= maxHeight; y++) {
                                    world.getBlockAt(fx, y, fz).setType(Material.AIR);
                                }
                            }
                        }.runTask(plugin);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void teleportToArea(Player player, BuildArea area) {
        Location spawnLoc = area.getCenter(SPAWN_Y);
        player.teleport(spawnLoc);
    }

    public Team getTeamAtLocation(Location location, List<Team> teams) {
        for (Team team : teams) {
            if (team.getArea() != null && team.getArea().contains(location)) {
                return team;
            }
        }
        return null;
    }
}
