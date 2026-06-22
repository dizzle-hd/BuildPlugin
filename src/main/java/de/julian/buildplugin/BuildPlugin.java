package de.julian.buildplugin;

import de.julian.buildplugin.commands.BuildCommand;
import de.julian.buildplugin.commands.VoteCommand;
import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.gui.SettingsGUI;
import de.julian.buildplugin.listeners.GameListener;
import de.julian.buildplugin.queue.QueueManager;
import de.julian.buildplugin.world.ArenaWorldManager;
import de.julian.buildplugin.world.VoidGenerator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class BuildPlugin extends JavaPlugin {

    private Game game;
    private QueueManager queueManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        game = new Game(this);
        queueManager = new QueueManager(this, game);

        SettingsGUI settingsGUI = new SettingsGUI(game);

        getServer().getPluginManager().registerEvents(
                new GameListener(game, settingsGUI, queueManager), this
        );

        Objects.requireNonNull(getCommand("build")).setExecutor(new BuildCommand(game, settingsGUI, queueManager));
        Objects.requireNonNull(getCommand("vote")).setExecutor(new VoteCommand(game));

        // Create arena world after 1 tick (worlds must be loaded first)
        getServer().getScheduler().runTaskLater(this, () -> game.getArenaWorld(), 1L);

        getLogger().info("BuildPlugin enabled! Arena world: " + ArenaWorldManager.WORLD_NAME);
    }

    @Override
    public void onDisable() {
        if (game != null) game.stopGame();
        if (queueManager != null) queueManager.shutdown();
        getLogger().info("BuildPlugin disabled.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (ArenaWorldManager.WORLD_NAME.equals(worldName)) {
            return new VoidGenerator();
        }
        return null;
    }

    public Game getGame() { return game; }
    public QueueManager getQueueManager() { return queueManager; }
}
