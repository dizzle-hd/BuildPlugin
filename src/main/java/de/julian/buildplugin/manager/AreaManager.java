package de.julian.buildplugin.manager;

import de.julian.buildplugin.game.BuildArea;
import de.julian.buildplugin.game.Team;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AreaManager {

    public List<BuildArea> createGrid(World world, int centerX, int centerZ, int areaSize) {
        List<BuildArea> areas = new ArrayList<>();

        // Team 1: top-left
        areas.add(new BuildArea(world, centerX - areaSize * 2, centerX - areaSize, centerZ - areaSize * 2, centerZ - areaSize));
        // Team 2: top-right
        areas.add(new BuildArea(world, centerX + areaSize, centerX + areaSize * 2, centerZ - areaSize * 2, centerZ - areaSize));
        // Team 3: bottom-left
        areas.add(new BuildArea(world, centerX - areaSize * 2, centerX - areaSize, centerZ + areaSize, centerZ + areaSize * 2));
        // Team 4: bottom-right
        areas.add(new BuildArea(world, centerX + areaSize, centerX + areaSize * 2, centerZ + areaSize, centerZ + areaSize * 2));

        return areas;
    }

    public void teleportToArea(Player player, BuildArea area) {
        int y = area.getWorld().getHighestBlockYAt(
                (area.getMinX() + area.getMaxX()) / 2,
                (area.getMinZ() + area.getMaxZ()) / 2
        ) + 1;
        Location spawnLoc = area.getCenter(y);
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
