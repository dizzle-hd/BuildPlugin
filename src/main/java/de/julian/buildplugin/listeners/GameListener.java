package de.julian.buildplugin.listeners;

import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.game.GameState;
import de.julian.buildplugin.game.Team;
import de.julian.buildplugin.gui.SettingsGUI;
import de.julian.buildplugin.queue.QueueManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private final Game game;
    private final SettingsGUI settingsGUI;
    private final QueueManager queueManager;

    public GameListener(Game game, SettingsGUI settingsGUI, QueueManager queueManager) {
        this.game = game;
        this.settingsGUI = settingsGUI;
        this.queueManager = queueManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.equals(SettingsGUI.GUI_TITLE)) {
            event.setCancelled(true);
            handleSettingsGUIClick(player, event.getSlot());
        }
    }

    private void handleSettingsGUIClick(Player player, int slot) {
        switch (slot) {
            case 10 -> { game.setBuildTime(30 * 60); settingsGUI.open(player); }
            case 11 -> { game.setBuildTime(45 * 60); settingsGUI.open(player); }
            case 12 -> { game.setBuildTime(60 * 60); settingsGUI.open(player); }
            case 13 -> { game.setBuildTime(90 * 60); settingsGUI.open(player); }
            case 14 -> { game.setBuildTime(120 * 60); settingsGUI.open(player); }
            case 19 -> { game.setVotingTime(5 * 60); settingsGUI.open(player); }
            case 20 -> { game.setVotingTime(10 * 60); settingsGUI.open(player); }
            case 21 -> { game.setVotingTime(15 * 60); settingsGUI.open(player); }
            case 28 -> { game.setSoloMode(!game.isSoloMode()); settingsGUI.open(player); }
            case 49 -> {
                player.closeInventory();
                if (game.getState() != GameState.WAITING) {
                    player.sendMessage(Component.text("A game is already running!", NamedTextColor.RED));
                    return;
                }
                if (game.getTeams().isEmpty()) {
                    player.sendMessage(Component.text("No players added!", NamedTextColor.RED));
                    return;
                }
                game.startBuildPhase();
            }
            case 45 -> {
                player.closeInventory();
                game.stopGame();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (game.getState() != GameState.BUILDING) return;

        Player player = event.getPlayer();
        Team team = game.getTeamOfPlayer(player.getUniqueId());
        if (team == null || team.getArea() == null) return;

        if (!team.getArea().contains(event.getTo())) {
            event.setTo(event.getFrom().clone());
            player.sendMessage(Component.text("You cannot leave your build area!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (game.getState() != GameState.BUILDING) return;

        Player player = event.getPlayer();
        Team team = game.getTeamOfPlayer(player.getUniqueId());
        if (team == null || team.getArea() == null) return;

        if (!team.getArea().contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You can only build in your own area!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (game.getState() != GameState.BUILDING) return;

        Player player = event.getPlayer();
        Team team = game.getTeamOfPlayer(player.getUniqueId());
        if (team == null || team.getArea() == null) return;

        if (!team.getArea().contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You can only build in your own area!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (game.getState() == GameState.WAITING || game.getState() == GameState.ENDED) return;
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        queueManager.removePlayer(player.getUniqueId());

        if (game.getState() != GameState.WAITING && game.getState() != GameState.ENDED) {
            Team team = game.getTeamOfPlayer(player.getUniqueId());
            if (team != null) team.getPlayers().remove(player.getUniqueId());
        }
    }
}
