package de.julian.buildplugin.game;

import de.julian.buildplugin.manager.AreaManager;
import de.julian.buildplugin.manager.WallManager;
import de.julian.buildplugin.world.ArenaWorldManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {

    private final Plugin plugin;
    private final AreaManager areaManager;
    private WallManager wallManager;
    private final ArenaWorldManager arenaWorldManager;

    private GameState state = GameState.WAITING;
    private final List<Team> teams = new ArrayList<>();

    private int buildTime;
    private int votingTime;
    private int wallHeight;
    private boolean soloMode;

    // Arena is always at 0,0 in the void world
    private static final int CENTER_X = 0;
    private static final int CENTER_Z = 0;

    private BukkitTask countdownTask;
    private BukkitTask actionBarTask;
    private int remainingSeconds;

    public Game(Plugin plugin) {
        this.plugin = plugin;
        this.arenaWorldManager = new ArenaWorldManager(plugin);
        loadConfig();
        this.areaManager = new AreaManager(plugin);
        this.wallManager = new WallManager(plugin, wallHeight);
    }

    private void loadConfig() {
        buildTime = plugin.getConfig().getInt("game.build-time", 3600);
        votingTime = plugin.getConfig().getInt("game.voting-time", 600);
        wallHeight = plugin.getConfig().getInt("game.wall-height", 64);
        soloMode = plugin.getConfig().getString("game.mode", "solo").equals("solo");
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
        wallManager = new WallManager(plugin, wallHeight);
    }

    public World getArenaWorld() {
        return arenaWorldManager.getOrCreateArenaWorld();
    }

    public void startBuildPhase() {
        if (state != GameState.WAITING) return;
        if (teams.isEmpty()) return;

        int areaSize = plugin.getConfig().getInt("game.area-size", 64);
        World arenaWorld = arenaWorldManager.getOrCreateArenaWorld();

        List<BuildArea> areas = areaManager.createGrid(arenaWorld, CENTER_X, CENTER_Z, areaSize);

        // Always generate ALL 4 platforms so the arena is complete and symmetric,
        // even when fewer than 4 teams are playing (e.g. /build test).
        for (BuildArea area : areas) {
            areaManager.generatePlatform(area);
        }

        // Assign the present teams to the first available plots.
        for (int i = 0; i < teams.size(); i++) {
            teams.get(i).setArea(areas.get(i % areas.size()));
        }

        // Outer barriers stay for the whole game; red divider is removed at voting start
        wallManager.placeOuterBarriers(arenaWorld, CENTER_X, CENTER_Z, areaSize);
        wallManager.placeRedDivider(arenaWorld, CENTER_X, CENTER_Z, areaSize);

        // Give players a moment for platforms to generate before teleporting
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Team team : teams) {
                for (UUID uuid : team.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        areaManager.teleportToArea(player, team.getArea());
                        player.setGameMode(GameMode.SURVIVAL);
                        player.setAllowFlight(false);
                        player.sendMessage(Component.text("Das Spiel beginnt! Du hast " + formatTime(buildTime) + " zum Bauen!", NamedTextColor.GREEN));
                    }
                }
            }
        }, 40L); // 2 second delay for platform generation

        broadcastAll(Component.text("=== BUILD PHASE GESTARTET ===", NamedTextColor.GOLD));
        state = GameState.BUILDING;
        startCountdown(buildTime, this::startVotingPhase);
        startActionBar();
    }

    public void startVotingPhase() {
        if (state != GameState.BUILDING) return;

        int areaSize = plugin.getConfig().getInt("game.area-size", 128);
        World arenaWorld = arenaWorldManager.getOrCreateArenaWorld();

        // Remove red divider so players can see all builds — outer barriers stay
        wallManager.removeRedDivider(arenaWorld, CENTER_X, CENTER_Z, areaSize);

        for (Team team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.sendMessage(Component.text("Bauzeit vorbei! Fliege zu den anderen Builds und stimme mit /vote <1-5> ab!", NamedTextColor.YELLOW));
                }
            }
        }

        broadcastAll(Component.text("=== VOTING PHASE GESTARTET ===", NamedTextColor.GOLD));
        broadcastAll(Component.text("Fliege zu einem Build und tippe /vote <1-5>!", NamedTextColor.YELLOW));
        state = GameState.VOTING;
        startCountdown(votingTime, this::endGame);
    }

    public void endGame() {
        if (countdownTask != null) countdownTask.cancel();
        if (actionBarTask != null) actionBarTask.cancel();
        state = GameState.ENDED;

        teams.sort((a, b) -> b.getScore() - a.getScore());

        broadcastAll(Component.text("=== SPIEL BEENDET ===", NamedTextColor.GOLD));
        broadcastAll(Component.text("Ergebnisse:", NamedTextColor.WHITE));

        int rank = 1;
        for (Team team : teams) {
            broadcastAll(Component.text(rank + ". " + team.getName() + " (" + getPlayerNames(team) + "): " + team.getScore() + " Punkte", NamedTextColor.AQUA));
            rank++;
        }

        // Teleport players back to main world after 10 seconds
        World mainWorld = Bukkit.getWorlds().get(0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Team team : teams) {
                for (UUID uuid : team.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        player.teleport(mainWorld.getSpawnLocation());
                    }
                }
            }
            teams.clear();
            state = GameState.WAITING;
        }, 200L); // 10 seconds
    }

    public void stopGame() {
        if (countdownTask != null) countdownTask.cancel();
        if (actionBarTask != null) actionBarTask.cancel();

        int areaSize = plugin.getConfig().getInt("game.area-size", 128);
        World arenaWorld = arenaWorldManager.getOrCreateArenaWorld();

        wallManager.removeOuterBarriers(arenaWorld, CENTER_X, CENTER_Z, areaSize);
        wallManager.removeRedDivider(arenaWorld, CENTER_X, CENTER_Z, areaSize);

        World mainWorld = Bukkit.getWorlds().get(0);
        for (Team team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.teleport(mainWorld.getSpawnLocation());
                }
            }
        }

        state = GameState.WAITING;
        teams.clear();
        broadcastAll(Component.text("Das Spiel wurde abgebrochen.", NamedTextColor.RED));
    }

    /**
     * Called on server shutdown. Cancels countdown and teleports players without scheduling any tasks.
     */
    public void onServerShutdown() {
        if (countdownTask != null) countdownTask.cancel();
        if (actionBarTask != null) actionBarTask.cancel();
        if (state == GameState.WAITING || state == GameState.ENDED) return;

        World mainWorld = Bukkit.getWorlds().get(0);
        for (Team team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.teleport(mainWorld.getSpawnLocation());
                }
            }
        }
        teams.clear();
        state = GameState.WAITING;
    }

    public boolean submitVote(Player voter, int points) {
        if (state != GameState.VOTING) {
            voter.sendMessage(Component.text("Abstimmen ist nur in der Voting-Phase moeglich!", NamedTextColor.RED));
            return false;
        }

        Team voterTeam = getTeamOfPlayer(voter.getUniqueId());
        Team targetTeam = areaManager.getTeamAtLocation(voter.getLocation(), teams);

        if (targetTeam == null) {
            voter.sendMessage(Component.text("Stehe in einem Build-Bereich um abzustimmen!", NamedTextColor.RED));
            return false;
        }
        if (targetTeam.equals(voterTeam)) {
            voter.sendMessage(Component.text("Du kannst nicht fuer deinen eigenen Build stimmen!", NamedTextColor.RED));
            return false;
        }
        if (targetTeam.hasVotedFrom(voter.getUniqueId())) {
            voter.sendMessage(Component.text("Du hast fuer diesen Build bereits abgestimmt!", NamedTextColor.RED));
            return false;
        }

        targetTeam.addVote(voter.getUniqueId(), points);
        voter.sendMessage(Component.text("Du hast " + points + " Punkt(e) fuer " + targetTeam.getName() + " vergeben!", NamedTextColor.GREEN));
        return true;
    }

    public Team addPlayerToTeam(Player player, String teamName) {
        Team team = getOrCreateTeam(teamName);
        if (!team.hasPlayer(player.getUniqueId())) {
            team.addPlayer(player);
        }
        return team;
    }

    private Team getOrCreateTeam(String name) {
        for (Team t : teams) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        Team newTeam = new Team(name);
        teams.add(newTeam);
        return newTeam;
    }

    public Team getTeamOfPlayer(UUID uuid) {
        for (Team team : teams) {
            if (team.hasPlayer(uuid)) return team;
        }
        return null;
    }

    private void startActionBar() {
        if (actionBarTask != null) actionBarTask.cancel();
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state == GameState.WAITING || state == GameState.ENDED) {
                actionBarTask.cancel();
                return;
            }
            Component bar = buildActionBar();
            for (Team team : teams) {
                for (UUID uuid : team.getPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) player.sendActionBar(bar);
                }
            }
        }, 0L, 20L);
    }

    private Component buildActionBar() {
        if (state == GameState.BUILDING) {
            return Component.text()
                    .append(Component.text("Build Phase", NamedTextColor.GREEN))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Time left: ", NamedTextColor.GRAY))
                    .append(Component.text(formatTime(remainingSeconds), NamedTextColor.YELLOW))
                    .build();
        }
        if (state == GameState.VOTING) {
            return Component.text()
                    .append(Component.text("Voting Phase", NamedTextColor.GOLD))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Time left: ", NamedTextColor.GRAY))
                    .append(Component.text(formatTime(remainingSeconds), NamedTextColor.YELLOW))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Stand on a plot & /vote <1-5>", NamedTextColor.WHITE))
                    .build();
        }
        return Component.empty();
    }

    private void startCountdown(int seconds, Runnable onFinish) {
        remainingSeconds = seconds;
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                countdownTask.cancel();
                onFinish.run();
                return;
            }
            if (remainingSeconds % 60 == 0 || remainingSeconds <= 10) {
                broadcastAll(Component.text("Verbleibende Zeit: " + formatTime(remainingSeconds), NamedTextColor.YELLOW));
            }
        }, 20L, 20L);
    }

    private void broadcastAll(Component message) {
        for (Team team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) player.sendMessage(message);
            }
        }
    }

    private String getPlayerNames(Team team) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : team.getPlayers()) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(name);
            }
        }
        return sb.toString();
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0 && sec > 0) return min + "m " + sec + "s";
        if (min > 0) return min + "m";
        return sec + "s";
    }

    public GameState getState() { return state; }
    public List<Team> getTeams() { return teams; }
    public int getBuildTime() { return buildTime; }
    public int getVotingTime() { return votingTime; }
    public boolean isSoloMode() { return soloMode; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public void setBuildTime(int seconds) { this.buildTime = seconds; }
    public void setVotingTime(int seconds) { this.votingTime = seconds; }
    public void setSoloMode(boolean solo) { this.soloMode = solo; }
}
