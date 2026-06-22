package de.julian.buildplugin.commands;

import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.game.GameState;
import de.julian.buildplugin.gui.SettingsGUI;
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

    public BuildCommand(Game game, SettingsGUI settingsGUI) {
        this.game = game;
        this.settingsGUI = settingsGUI;
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
                    player.sendMessage(Component.text("Keine Spieler hinzugefuegt! Nutze /build addplayer <spieler> <team>", NamedTextColor.RED));
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
                String teamName = args[2];
                game.addPlayerToTeam(target, teamName);
                player.sendMessage(Component.text(target.getName() + " wurde Team '" + teamName + "' hinzugefuegt.", NamedTextColor.GREEN));
                target.sendMessage(Component.text("Du wurdest Team '" + teamName + "' hinzugefuegt.", NamedTextColor.GREEN));
            }

            case "status" -> {
                player.sendMessage(Component.text("=== BuildPlugin Status ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text("Status: " + game.getState(), NamedTextColor.WHITE));
                player.sendMessage(Component.text("Teams: " + game.getTeams().size(), NamedTextColor.WHITE));
                if (game.getState() == GameState.BUILDING || game.getState() == GameState.VOTING) {
                    player.sendMessage(Component.text("Verbleibende Zeit: " + formatTime(game.getRemainingSeconds()), NamedTextColor.YELLOW));
                }
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== BuildPlugin Befehle ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/build gui - Einstellungs-GUI oeffnen", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build start - Spiel starten", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build stop - Spiel abbrechen", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build addplayer <spieler> <team> - Spieler hinzufuegen", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build status - Spielstatus anzeigen", NamedTextColor.WHITE));
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0) return min + "m " + sec + "s";
        return sec + "s";
    }
}
