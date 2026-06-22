package de.julian.buildplugin.commands;

import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.game.GameState;
import de.julian.buildplugin.gui.SettingsGUI;
import de.julian.buildplugin.npc.NPCData;
import de.julian.buildplugin.npc.NPCManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuildCommand implements CommandExecutor {

    private final Game game;
    private final SettingsGUI settingsGUI;
    private final NPCManager npcManager;

    public BuildCommand(Game game, SettingsGUI settingsGUI, NPCManager npcManager) {
        this.game = game;
        this.settingsGUI = settingsGUI;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler koennen diesen Befehl verwenden.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> settingsGUI.open(player);

            case "start" -> {
                if (game.getState() != GameState.WAITING) {
                    player.sendMessage(Component.text("Ein Spiel laeuft bereits!", NamedTextColor.RED));
                    return true;
                }
                if (game.getTeams().isEmpty()) {
                    player.sendMessage(Component.text("Keine Spieler hinzugefuegt! Tritt ueber einen NPC bei oder nutze /build addplayer", NamedTextColor.RED));
                    return true;
                }
                game.startBuildPhase();
            }

            case "stop" -> {
                if (game.getState() == GameState.WAITING) {
                    player.sendMessage(Component.text("Es laeuft kein Spiel.", NamedTextColor.RED));
                    return true;
                }
                game.stopGame();
            }

            case "addplayer" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Verwendung: /build addplayer <spieler> <teamname>", NamedTextColor.RED));
                    return true;
                }
                if (game.getState() != GameState.WAITING) {
                    player.sendMessage(Component.text("Spieler koennen nur vor dem Spiel hinzugefuegt werden.", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(Component.text("Spieler '" + args[1] + "' nicht gefunden.", NamedTextColor.RED));
                    return true;
                }
                game.addPlayerToTeam(target, args[2]);
                player.sendMessage(Component.text(target.getName() + " wurde Team '" + args[2] + "' hinzugefuegt.", NamedTextColor.GREEN));
                target.sendMessage(Component.text("Du wurdest Team '" + args[2] + "' hinzugefuegt.", NamedTextColor.GREEN));
            }

            case "npc" -> handleNPCCommand(player, args);

            case "status" -> {
                player.sendMessage(Component.text("=== BuildPlugin Status ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text("Status: " + game.getState(), NamedTextColor.WHITE));
                player.sendMessage(Component.text("Teams: " + game.getTeams().size(), NamedTextColor.WHITE));
                if (game.getState() == GameState.BUILDING || game.getState() == GameState.VOTING) {
                    player.sendMessage(Component.text("Verbleibende Zeit: " + formatTime(game.getRemainingSeconds()), NamedTextColor.YELLOW));
                }
                player.sendMessage(Component.text("NPCs: " + npcManager.getNPCs().size(), NamedTextColor.WHITE));
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void handleNPCCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Verwendung: /build npc <place|remove|list>", NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "place" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Verwendung: /build npc place <minuten>", NamedTextColor.RED));
                    player.sendMessage(Component.text("Erlaubte Werte: 30, 45, 60, 90, 120", NamedTextColor.GRAY));
                    return;
                }
                int minutes;
                try {
                    minutes = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Ungueltige Minutenzahl.", NamedTextColor.RED));
                    return;
                }
                if (minutes != 30 && minutes != 45 && minutes != 60 && minutes != 90 && minutes != 120) {
                    player.sendMessage(Component.text("Erlaubte Werte: 30, 45, 60, 90, 120", NamedTextColor.RED));
                    return;
                }
                NPCData npc = npcManager.spawnNPC(player.getLocation(), minutes);
                player.sendMessage(Component.text("NPC fuer " + minutes + " Minuten wurde an deiner Position platziert!", NamedTextColor.GREEN));
            }

            case "remove" -> {
                NPCData removed = npcManager.removeNearestNPC(player.getLocation(), 10.0);
                if (removed == null) {
                    player.sendMessage(Component.text("Kein NPC in der Naehe (max. 10 Bloecke).", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("NPC (" + removed.getBuildTimeMinutes() + " min) wurde entfernt.", NamedTextColor.GREEN));
                }
            }

            case "list" -> {
                if (npcManager.getNPCs().isEmpty()) {
                    player.sendMessage(Component.text("Keine NPCs vorhanden.", NamedTextColor.YELLOW));
                    return;
                }
                player.sendMessage(Component.text("=== Platzierte NPCs ===", NamedTextColor.GOLD));
                for (NPCData npc : npcManager.getNPCs()) {
                    player.sendMessage(Component.text("- " + npc.getBuildTimeMinutes() + " min @ "
                            + npc.getWorldName() + " "
                            + (int) npc.getX() + "/" + (int) npc.getY() + "/" + (int) npc.getZ(),
                            NamedTextColor.WHITE));
                }
            }

            default -> player.sendMessage(Component.text("Verwendung: /build npc <place|remove|list>", NamedTextColor.RED));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== BuildPlugin Befehle ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/build gui - Einstellungs-GUI", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build start - Spiel starten", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build stop - Spiel abbrechen", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build addplayer <spieler> <team> - Spieler hinzufuegen", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build npc place <30|45|60|90|120> - NPC platzieren", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build npc remove - Naechsten NPC entfernen", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build npc list - Alle NPCs auflisten", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build status - Spielstatus", NamedTextColor.WHITE));
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0) return min + "m " + sec + "s";
        return sec + "s";
    }
}
