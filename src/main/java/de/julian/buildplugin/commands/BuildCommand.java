package de.julian.buildplugin.commands;

import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.game.GameState;
import de.julian.buildplugin.gui.SettingsGUI;
import de.julian.buildplugin.queue.QueueManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BuildCommand implements TabExecutor {

    private final Game game;
    private final SettingsGUI settingsGUI;
    private final QueueManager queueManager;

    public BuildCommand(Game game, SettingsGUI settingsGUI, QueueManager queueManager) {
        this.game = game;
        this.settingsGUI = settingsGUI;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── Queue commands (for players / NPC plugins) ──────────────────
            case "join" -> {
                boolean solo = true;
                int minutes = game.getBuildTime() / 60;

                if (args.length >= 2) {
                    solo = !args[1].equalsIgnoreCase("team");
                }
                if (args.length >= 3) {
                    try {
                        minutes = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Invalid time. Use: /build join <solo|team> [minutes]", NamedTextColor.RED));
                        return true;
                    }
                }
                queueManager.joinQueue(player, solo, minutes);
            }

            case "leave" -> queueManager.leaveQueue(player);

            case "queue" -> {
                if (queueManager.getQueues().isEmpty()) {
                    player.sendMessage(Component.text("All queues are empty.", NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("=== Active Queues ===", NamedTextColor.GOLD));
                    queueManager.getQueues().forEach((key, entries) -> {
                        if (!entries.isEmpty()) {
                            player.sendMessage(Component.text(
                                    key + ": " + entries.size() + "/4 players", NamedTextColor.WHITE));
                        }
                    });
                }
            }

            // ── Admin commands ───────────────────────────────────────────────
            case "gui" -> settingsGUI.open(player);

            case "start" -> {
                if (game.getState() != GameState.WAITING) {
                    player.sendMessage(Component.text("A game is already running!", NamedTextColor.RED));
                    return true;
                }
                if (game.getTeams().isEmpty()) {
                    player.sendMessage(Component.text("No players added. Use /build addplayer or /build join.", NamedTextColor.RED));
                    return true;
                }
                game.startBuildPhase();
            }

            case "stop" -> {
                if (game.getState() == GameState.WAITING) {
                    player.sendMessage(Component.text("No game is running.", NamedTextColor.RED));
                    return true;
                }
                game.stopGame();
            }

            case "addplayer" -> {
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /build addplayer <player> <team>", NamedTextColor.RED));
                    return true;
                }
                if (game.getState() != GameState.WAITING) {
                    player.sendMessage(Component.text("Cannot add players during a game.", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(Component.text("Player '" + args[1] + "' not found.", NamedTextColor.RED));
                    return true;
                }
                game.addPlayerToTeam(target, args[2]);
                player.sendMessage(Component.text(target.getName() + " added to team '" + args[2] + "'.", NamedTextColor.GREEN));
                target.sendMessage(Component.text("You were added to team '" + args[2] + "'.", NamedTextColor.GREEN));
            }

            // Starts a test game with just yourself to verify map generation
            case "test" -> {
                if (game.getState() != GameState.WAITING) {
                    player.sendMessage(Component.text("A game is already running!", NamedTextColor.RED));
                    return true;
                }
                game.setBuildTime(120);   // 2 minutes
                game.setVotingTime(60);   // 1 minute
                game.setSoloMode(true);
                game.addPlayerToTeam(player, player.getName());
                game.startBuildPhase();
                player.sendMessage(Component.text(
                        "Test game started! 2 min build, 1 min voting. Check if your platform generates correctly.",
                        NamedTextColor.AQUA));
            }

            case "status" -> {
                player.sendMessage(Component.text("=== BuildPlugin Status ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text("State: " + game.getState(), NamedTextColor.WHITE));
                player.sendMessage(Component.text("Teams: " + game.getTeams().size(), NamedTextColor.WHITE));
                if (game.getState() == GameState.BUILDING || game.getState() == GameState.VOTING) {
                    player.sendMessage(Component.text("Time left: " + formatTime(game.getRemainingSeconds()), NamedTextColor.YELLOW));
                }
                int totalQueued = queueManager.getQueues().values().stream().mapToInt(java.util.List::size).sum();
                player.sendMessage(Component.text("Players in queues: " + totalQueued, NamedTextColor.WHITE));
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== BuildPlugin Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/build join <solo|team> [minutes] - Join the queue", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build leave - Leave the queue", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build queue - Show active queues", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build test - Start a test game (admin)", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build gui - Settings GUI (admin)", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build start - Start game manually (admin)", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build stop - Stop game (admin)", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build addplayer <player> <team> - Add player manually (admin)", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/build status - Show game status", NamedTextColor.WHITE));
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0) return min + "m " + sec + "s";
        return sec + "s";
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return List.of();

        if (args.length == 1) {
            List<String> subs = List.of("join", "leave", "queue", "test", "gui", "start", "stop", "addplayer", "status");
            return StringUtil.copyPartialMatches(args[0], subs, new ArrayList<>());
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "join" -> StringUtil.copyPartialMatches(args[1], List.of("solo", "team"), new ArrayList<>());
                case "addplayer" -> {
                    List<String> names = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    yield StringUtil.copyPartialMatches(args[1], names, new ArrayList<>());
                }
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase()) {
                case "join" -> StringUtil.copyPartialMatches(args[2],
                        List.of("30", "45", "60", "90", "120"), new ArrayList<>());
                case "addplayer" -> {
                    List<String> teams = List.of("Rot", "Blau", "Gruen", "Gelb");
                    yield StringUtil.copyPartialMatches(args[2], teams, new ArrayList<>());
                }
                default -> List.of();
            };
        }

        return List.of();
    }
}
