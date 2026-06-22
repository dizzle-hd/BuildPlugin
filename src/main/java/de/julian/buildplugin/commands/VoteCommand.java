package de.julian.buildplugin.commands;

import de.julian.buildplugin.game.Game;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VoteCommand implements TabExecutor {

    private final Game game;

    public VoteCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can vote.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /vote <1-5>", NamedTextColor.RED));
            return true;
        }

        int points;
        try {
            points = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Please enter a number between 1 and 5.", NamedTextColor.RED));
            return true;
        }

        if (points < 1 || points > 5) {
            player.sendMessage(Component.text("Please enter a number between 1 and 5.", NamedTextColor.RED));
            return true;
        }

        game.submitVote(player, points);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("1", "2", "3", "4", "5"), new ArrayList<>());
        }
        return List.of();
    }
}
