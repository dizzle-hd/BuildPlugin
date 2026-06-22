package de.julian.buildplugin.listeners;

import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.game.GameState;
import de.julian.buildplugin.game.Team;
import de.julian.buildplugin.gui.NPCJoinGUI;
import de.julian.buildplugin.gui.SettingsGUI;
import de.julian.buildplugin.npc.NPCData;
import de.julian.buildplugin.npc.NPCManager;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {

    private final Game game;
    private final SettingsGUI settingsGUI;
    private final NPCManager npcManager;
    private final NPCJoinGUI npcJoinGUI;

    public GameListener(Game game, SettingsGUI settingsGUI, NPCManager npcManager, NPCJoinGUI npcJoinGUI) {
        this.game = game;
        this.settingsGUI = settingsGUI;
        this.npcManager = npcManager;
        this.npcJoinGUI = npcJoinGUI;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        NPCData npcData = npcManager.getNPCByEntity(event.getRightClicked().getUniqueId());
        if (npcData == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (game.getState() != GameState.WAITING) {
            player.sendMessage(Component.text("Es laeuft bereits ein Spiel!", NamedTextColor.RED));
            return;
        }

        npcJoinGUI.open(player, npcData.getBuildTimeMinutes());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.equals(SettingsGUI.GUI_TITLE)) {
            event.setCancelled(true);
            handleSettingsGUIClick(player, event.getSlot());
            return;
        }

        if (npcJoinGUI.isNPCJoinTitle(title)) {
            event.setCancelled(true);
            int minutes = npcJoinGUI.extractMinutes(title);
            if (minutes <= 0) return;
            handleNPCJoinClick(player, event.getSlot(), minutes);
        }
    }

    private void handleNPCJoinClick(Player player, int slot, int buildTimeMinutes) {
        if (game.getState() != GameState.WAITING) {
            player.closeInventory();
            player.sendMessage(Component.text("Es laeuft bereits ein Spiel!", NamedTextColor.RED));
            return;
        }

        switch (slot) {
            case 11 -> {
                // Solo
                player.closeInventory();
                game.setBuildTime(buildTimeMinutes * 60);
                game.setSoloMode(true);
                game.addPlayerToTeam(player, player.getName());
                player.sendMessage(Component.text("Du wurdest als Solo-Spieler eingetragen! Warte auf weitere Spieler oder starte mit /build start", NamedTextColor.GREEN));
                notifyWaiting(player.getName() + " ist beigetreten (Solo, " + buildTimeMinutes + " min). Spieler: " + countPlayers());
            }
            case 15 -> {
                // Teams
                player.closeInventory();
                game.setBuildTime(buildTimeMinutes * 60);
                game.setSoloMode(false);
                // Simple team assignment: find a team with < 2 players, or create new one
                String teamName = findOrCreateTeam();
                game.addPlayerToTeam(player, teamName);
                player.sendMessage(Component.text("Du wurdest Team '" + teamName + "' hinzugefuegt! Warte auf weitere Spieler oder starte mit /build start", NamedTextColor.GREEN));
                notifyWaiting(player.getName() + " ist Team '" + teamName + "' beigetreten (" + buildTimeMinutes + " min). Spieler: " + countPlayers());
            }
        }
    }

    private String findOrCreateTeam() {
        String[] teamNames = {"Rot", "Blau", "Gruen", "Gelb"};
        for (String name : teamNames) {
            de.julian.buildplugin.game.Team team = game.getTeams().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
            if (team == null || team.getPlayers().size() < 2) {
                return name;
            }
        }
        return "Team" + (game.getTeams().size() + 1);
    }

    private int countPlayers() {
        return game.getTeams().stream().mapToInt(t -> t.getPlayers().size()).sum();
    }

    private void notifyWaiting(String message) {
        // Broadcast to all players in waiting teams
        game.getTeams().forEach(team ->
                team.getPlayers().forEach(uuid -> {
                    Player p = org.bukkit.Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage(Component.text(message, NamedTextColor.YELLOW));
                })
        );
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
                    player.sendMessage(Component.text("Ein Spiel laeuft bereits!", NamedTextColor.RED));
                    return;
                }
                if (game.getTeams().isEmpty()) {
                    player.sendMessage(Component.text("Keine Spieler hinzugefuegt!", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Du darfst deinen Baubereich nicht verlassen!", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Du kannst nur in deinem eigenen Baubereich bauen!", NamedTextColor.RED));
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
            player.sendMessage(Component.text("Du kannst nur in deinem eigenen Baubereich bauen!", NamedTextColor.RED));
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
        if (game.getState() == GameState.WAITING || game.getState() == GameState.ENDED) return;
        Player player = event.getPlayer();
        Team team = game.getTeamOfPlayer(player.getUniqueId());
        if (team != null) {
            team.getPlayers().remove(player.getUniqueId());
        }
    }
}
