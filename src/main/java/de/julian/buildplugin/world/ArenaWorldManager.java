package de.julian.buildplugin.world;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class ArenaWorldManager {

    public static final String WORLD_NAME = "build_arena";

    private final Plugin plugin;

    public ArenaWorldManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public World getOrCreateArenaWorld() {
        World existing = plugin.getServer().getWorld(WORLD_NAME);
        if (existing != null) return existing;

        World world = new WorldCreator(WORLD_NAME)
                .generator(new VoidGenerator())
                .generateStructures(false)
                .createWorld();

        if (world != null) {
            world.setSpawnLocation(0, 65, 0);
            world.setTime(6000); // Always midday
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, true);
            plugin.getLogger().info("Arena-Welt 'build_arena' wurde erstellt.");
        }

        return world;
    }

    /**
     * Unloads and deletes the arena world, then recreates it fresh.
     * Call this between games for a clean arena.
     */
    public World resetArenaWorld() {
        World world = plugin.getServer().getWorld(WORLD_NAME);
        if (world != null) {
            // Teleport any remaining players out
            World fallback = plugin.getServer().getWorlds().get(0);
            for (org.bukkit.entity.Player p : world.getPlayers()) {
                p.teleport(fallback.getSpawnLocation());
            }
            plugin.getServer().unloadWorld(world, false);
        }

        deleteWorldFolder(new File(plugin.getServer().getWorldContainer(), WORLD_NAME));
        return getOrCreateArenaWorld();
    }

    private void deleteWorldFolder(File folder) {
        if (!folder.exists()) return;
        try {
            Files.walk(folder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Arena-Welt nicht loeschen: " + e.getMessage());
        }
    }
}
