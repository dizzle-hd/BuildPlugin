package de.julian.buildplugin.commands;

import de.julian.buildplugin.game.Game;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VoteCommand implements CommandExecutor {

    private final Game game;

    public VoteCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler koennen abstimmen.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Verwendung: /vote <1-5>", NamedTextColor.RED));
            return true;
        }

        int points;
        try {
            points = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Bitte gib eine Zahl zwischen 1 und 5 ein.", NamedTextColor.RED));
            return true;
        }

        if (points < 1 || points > 5) {
            player.sendMessage(Component.text("Bitte gib eine Zahl zwischen 1 und 5 ein.", NamedTextColor.RED));
            return true;
        }

        game.submitVote(player, points);
        return true;
    }
}
