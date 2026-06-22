package de.julian.buildplugin.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NPCManager {

    private final Plugin plugin;
    private final List<NPCData> npcs = new ArrayList<>();

    public NPCManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public NPCData spawnNPC(Location location, int buildTimeMinutes) {
        String id = "npc_" + buildTimeMinutes + "_" + System.currentTimeMillis();
        NPCData data = new NPCData(id, location, buildTimeMinutes);

        Villager villager = spawnVillager(location, buildTimeMinutes);
        data.setEntityUUID(villager.getUniqueId());
        npcs.add(data);

        saveToConfig();
        return data;
    }

    private Villager spawnVillager(Location location, int buildTimeMinutes) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setPersistent(true);
        villager.setSilent(true);
        villager.setVillagerType(getVillagerTypeForTime(buildTimeMinutes));
        villager.setProfession(Villager.Profession.TOOLSMITH);
        villager.customName(Component.text(buildTimeMinutes + " Minuten", NamedTextColor.GOLD));
        villager.setCustomNameVisible(true);

        // Prevent natural despawn and max health
        if (villager.getAttribute(Attribute.MAX_HEALTH) != null) {
            villager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2048);
            villager.setHealth(2048);
        }

        return villager;
    }

    private Villager.Type getVillagerTypeForTime(int minutes) {
        return switch (minutes) {
            case 30 -> Villager.Type.PLAINS;
            case 45 -> Villager.Type.SAVANNA;
            case 60 -> Villager.Type.DESERT;
            case 90 -> Villager.Type.TAIGA;
            case 120 -> Villager.Type.SNOW;
            default -> Villager.Type.PLAINS;
        };
    }

    public void removeNPC(UUID entityUUID) {
        npcs.removeIf(data -> {
            if (entityUUID.equals(data.getEntityUUID())) {
                // Remove the entity from the world
                Entity entity = plugin.getServer().getEntity(entityUUID);
                if (entity != null) entity.remove();
                return true;
            }
            return false;
        });
        saveToConfig();
    }

    public NPCData removeNearestNPC(Location location, double maxDistance) {
        NPCData nearest = null;
        double minDist = Double.MAX_VALUE;

        for (NPCData data : npcs) {
            Entity entity = plugin.getServer().getEntity(data.getEntityUUID());
            if (entity != null) {
                double dist = entity.getLocation().distance(location);
                if (dist < minDist && dist <= maxDistance) {
                    minDist = dist;
                    nearest = data;
                }
            }
        }

        if (nearest != null) {
            removeNPC(nearest.getEntityUUID());
        }
        return nearest;
    }

    public NPCData getNPCByEntity(UUID entityUUID) {
        return npcs.stream()
                .filter(d -> entityUUID.equals(d.getEntityUUID()))
                .findFirst()
                .orElse(null);
    }

    public void loadFromConfig() {
        var config = plugin.getConfig();
        var section = config.getConfigurationSection("npcs");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String worldName = section.getString(key + ".world");
            double x = section.getDouble(key + ".x");
            double y = section.getDouble(key + ".y");
            double z = section.getDouble(key + ".z");
            float yaw = (float) section.getDouble(key + ".yaw");
            int minutes = section.getInt(key + ".minutes");

            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) continue;

            Location loc = new Location(world, x, y, z, yaw, 0);
            NPCData data = new NPCData(key, loc, minutes);

            Villager villager = spawnVillager(loc, minutes);
            data.setEntityUUID(villager.getUniqueId());
            npcs.add(data);
        }
    }

    public void removeAllNPCs() {
        for (NPCData data : npcs) {
            Entity entity = plugin.getServer().getEntity(data.getEntityUUID());
            if (entity != null) entity.remove();
        }
        npcs.clear();
    }

    private void saveToConfig() {
        plugin.getConfig().set("npcs", null);
        for (NPCData data : npcs) {
            String path = "npcs." + data.getId();
            plugin.getConfig().set(path + ".world", data.getWorldName());
            plugin.getConfig().set(path + ".x", data.getX());
            plugin.getConfig().set(path + ".y", data.getY());
            plugin.getConfig().set(path + ".z", data.getZ());
            plugin.getConfig().set(path + ".yaw", data.getYaw());
            plugin.getConfig().set(path + ".minutes", data.getBuildTimeMinutes());
        }
        plugin.saveConfig();
    }

    public List<NPCData> getNPCs() { return npcs; }
}
