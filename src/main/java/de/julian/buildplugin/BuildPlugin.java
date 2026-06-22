package de.julian.buildplugin;

import de.julian.buildplugin.commands.BuildCommand;
import de.julian.buildplugin.commands.VoteCommand;
import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.gui.NPCJoinGUI;
import de.julian.buildplugin.gui.SettingsGUI;
import de.julian.buildplugin.listeners.GameListener;
import de.julian.buildplugin.npc.NPCManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class BuildPlugin extends JavaPlugin {

    private Game game;
    private NPCManager npcManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        game = new Game(this);
        npcManager = new NPCManager(this);

        SettingsGUI settingsGUI = new SettingsGUI(game);
        NPCJoinGUI npcJoinGUI = new NPCJoinGUI(game);

        getServer().getPluginManager().registerEvents(
                new GameListener(game, settingsGUI, npcManager, npcJoinGUI), this
        );

        Objects.requireNonNull(getCommand("build")).setExecutor(new BuildCommand(game, settingsGUI, npcManager));
        Objects.requireNonNull(getCommand("vote")).setExecutor(new VoteCommand(game));

        // Load saved NPCs after world is ready (1 tick delay)
        getServer().getScheduler().runTaskLater(this, npcManager::loadFromConfig, 1L);

        getLogger().info("BuildPlugin wurde aktiviert!");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.stopGame();
        }
        if (npcManager != null) {
            npcManager.removeAllNPCs();
        }
        getLogger().info("BuildPlugin wurde deaktiviert!");
    }

    public Game getGame() { return game; }
    public NPCManager getNpcManager() { return npcManager; }
}
