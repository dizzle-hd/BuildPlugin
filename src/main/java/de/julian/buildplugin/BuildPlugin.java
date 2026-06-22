package de.julian.buildplugin;

import de.julian.buildplugin.commands.BuildCommand;
import de.julian.buildplugin.commands.VoteCommand;
import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.gui.SettingsGUI;
import de.julian.buildplugin.listeners.GameListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class BuildPlugin extends JavaPlugin {

    private Game game;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        game = new Game(this);
        SettingsGUI settingsGUI = new SettingsGUI(game);

        getServer().getPluginManager().registerEvents(new GameListener(game, settingsGUI), this);

        Objects.requireNonNull(getCommand("build")).setExecutor(new BuildCommand(game, settingsGUI));
        Objects.requireNonNull(getCommand("vote")).setExecutor(new VoteCommand(game));

        getLogger().info("BuildPlugin wurde aktiviert!");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.stopGame();
        }
        getLogger().info("BuildPlugin wurde deaktiviert!");
    }

    public Game getGame() {
        return game;
    }
}
