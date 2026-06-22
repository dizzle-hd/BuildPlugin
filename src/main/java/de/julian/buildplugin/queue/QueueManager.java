package de.julian.buildplugin.queue;

import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueueManager {

    private static final int REQUIRED_PLAYERS = 4;

    private final Plugin plugin;
    private final Game game;

    // key = "solo_60" or "team_30" etc.
    private final Map<String, List<QueueEntry>> queues = new LinkedHashMap<>();
    private BukkitTask actionBarTask;

    public QueueManager(Plugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        startActionBarTask();
    }

    public boolean joinQueue(Player player, boolean solo, int buildTimeMinutes) {
        if (game.getState() != GameState.WAITING) {
            player.sendMessage(Component.text("A game is already running! Please wait.", NamedTextColor.RED));
            return false;
        }
        if (isInQueue(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a queue! Use /build leave to leave.", NamedTextColor.RED));
            return false;
        }

        QueueEntry entry = new QueueEntry(player.getUniqueId(), solo, buildTimeMinutes);
        queues.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry);

        int current = queues.get(entry.getKey()).size();
        player.sendMessage(Component.text(
                "You joined the " + (solo ? "Solo" : "Team") + " queue (" + buildTimeMinutes + " min). "
                + "[" + current + "/" + REQUIRED_PLAYERS + " players]",
                NamedTextColor.GREEN));

        // Notify others in same queue
        notifyQueue(entry.getKey(), player.getName() + " joined the queue. [" + current + "/" + REQUIRED_PLAYERS + "]");

        if (current >= REQUIRED_PLAYERS) {
            startGame(entry.getKey(), solo, buildTimeMinutes);
        }

        return true;
    }

    public boolean leaveQueue(Player player) {
        for (Map.Entry<String, List<QueueEntry>> entry : queues.entrySet()) {
            boolean removed = entry.getValue().removeIf(e -> e.getPlayerUUID().equals(player.getUniqueId()));
            if (removed) {
                int remaining = entry.getValue().size();
                if (entry.getValue().isEmpty()) {
                    queues.remove(entry.getKey());
                } else {
                    notifyQueue(entry.getKey(), player.getName() + " left the queue. [" + remaining + "/" + REQUIRED_PLAYERS + "]");
                }
                player.sendMessage(Component.text("You left the queue.", NamedTextColor.YELLOW));
                return true;
            }
        }
        player.sendMessage(Component.text("You are not in a queue.", NamedTextColor.RED));
        return false;
    }

    public void removePlayer(UUID uuid) {
        queues.values().forEach(list -> list.removeIf(e -> e.getPlayerUUID().equals(uuid)));
        queues.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public boolean isInQueue(UUID uuid) {
        return queues.values().stream()
                .flatMap(List::stream)
                .anyMatch(e -> e.getPlayerUUID().equals(uuid));
    }

    public QueueEntry getEntry(UUID uuid) {
        return queues.values().stream()
                .flatMap(List::stream)
                .filter(e -> e.getPlayerUUID().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    private void startGame(String key, boolean solo, int buildTimeMinutes) {
        List<QueueEntry> entries = queues.remove(key);
        if (entries == null || entries.size() < REQUIRED_PLAYERS) return;

        List<QueueEntry> chosen = entries.subList(0, REQUIRED_PLAYERS);

        game.setBuildTime(buildTimeMinutes * 60);
        game.setSoloMode(solo);

        if (solo) {
            for (QueueEntry entry : chosen) {
                Player player = Bukkit.getPlayer(entry.getPlayerUUID());
                if (player != null) {
                    game.addPlayerToTeam(player, player.getName());
                }
            }
        } else {
            String[] teamNames = {"Rot", "Blau"};
            for (int i = 0; i < chosen.size(); i++) {
                Player player = Bukkit.getPlayer(chosen.get(i).getPlayerUUID());
                if (player != null) {
                    game.addPlayerToTeam(player, teamNames[i / 2]);
                }
            }
        }

        Bukkit.broadcast(Component.text(
                "=== BUILD BATTLE STARTING! " + buildTimeMinutes + " min | "
                + (solo ? "Solo" : "Teams") + " ===",
                NamedTextColor.GOLD));

        game.startBuildPhase();
    }

    private void notifyQueue(String key, String message) {
        List<QueueEntry> entries = queues.get(key);
        if (entries == null) return;
        for (QueueEntry entry : entries) {
            Player p = Bukkit.getPlayer(entry.getPlayerUUID());
            if (p != null) {
                p.sendMessage(Component.text(message, NamedTextColor.YELLOW));
            }
        }
    }

    private void startActionBarTask() {
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            queues.forEach((key, entries) -> {
                entries.removeIf(e -> Bukkit.getPlayer(e.getPlayerUUID()) == null);
                if (entries.isEmpty()) return;

                QueueEntry sample = entries.get(0);
                int current = entries.size();
                String mode = sample.isSolo() ? "Solo" : "Team";
                int minutes = sample.getBuildTimeMinutes();

                Component actionBar = Component.text(
                        "⏳ You are in a queue (" + mode + " | " + minutes + " min). Please wait. ["
                        + current + "/" + REQUIRED_PLAYERS + " players]",
                        NamedTextColor.YELLOW);

                for (QueueEntry entry : entries) {
                    Player p = Bukkit.getPlayer(entry.getPlayerUUID());
                    if (p != null) p.sendActionBar(actionBar);
                }
            });
            queues.entrySet().removeIf(e -> e.getValue().isEmpty());
        }, 0L, 20L);
    }

    public void shutdown() {
        if (actionBarTask != null) actionBarTask.cancel();
    }

    public int getQueueSize(boolean solo, int buildTimeMinutes) {
        String key = (solo ? "solo" : "team") + "_" + buildTimeMinutes;
        List<QueueEntry> entries = queues.get(key);
        return entries == null ? 0 : entries.size();
    }

    public Map<String, List<QueueEntry>> getQueues() { return queues; }
}
