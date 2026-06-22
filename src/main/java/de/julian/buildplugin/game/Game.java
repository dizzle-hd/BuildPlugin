package de.julian.buildplugin.game;

import de.julian.buildplugin.manager.AreaManager;
import de.julian.buildplugin.manager.WallManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

    private GameState state = GameState.WAITING;
    private final List<Team> teams = new ArrayList<>();

    private int buildTime;
    private int votingTime;
    private int areaSize;
    private int wallHeight;
    private int centerX;
    private int centerZ;
    private World world;
    private boolean soloMode;

    private BukkitTask countdownTask;
    private int remainingSeconds;

    public Game(Plugin plugin) {
        this.plugin = plugin;
        this.areaManager = new AreaManager();
        loadConfig();
        this.wallManager = new WallManager(plugin, wallHeight);
    }

    private void loadConfig() {
        buildTime = plugin.getConfig().getInt("game.build-time", 3600);
        votingTime = plugin.getConfig().getInt("game.voting-time", 600);
        areaSize = plugin.getConfig().getInt("game.area-size", 128);
        wallHeight = plugin.getConfig().getInt("game.wall-height", 320);
        centerX = plugin.getConfig().getInt("game.center-x", 0);
        centerZ = plugin.getConfig().getInt("game.center-z", 0);
        String worldName = plugin.getConfig().getString("game.world", "world");
        world = Bukkit.getWorld(worldName);
        soloMode = plugin.getConfig().getString("game.mode", "solo").equals("solo");
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
        wallManager = new WallManager(plugin, wallHeight);
    }

    public void startBuildPhase() {
        if (state != GameState.WAITING) return;
        if (teams.isEmpty()) return;

        List<BuildArea> areas = areaManager.createGrid(world, centerX, centerZ, areaSize);

        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            BuildArea area = areas.get(i % areas.size());
            team.setArea(area);
        }

        wallManager.placeOuterWalls(world, centerX, centerZ, areaSize * 4);
        wallManager.placeInnerWalls(world, centerX, centerZ, areaSize * 4);

        for (Team team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    areaManager.teleportToArea(player, team.getArea());
                    player.setAllowFlight(false);
                    player.sendMessage(Component.text("Das Spiel beginnt! Du hast " + formatTime(buildTime) + " zum Bauen!", NamedTextColor.GREEN));
                }
            }
        }

        broadcastAll(Component.text("=== BUILD PHASE GESTARTET ===", NamedTextColor.GOLD));
        state = GameState.BUILDING;
        startCountdown(buildTime, this::startVotingPhase);
    }

    public void startVotingPhase() {
        if (state != GameState.BUILDING) return;

        wallManager.removeOuterWalls(world, centerX, centerZ, areaSize * 4);

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
        broadcastAll(Component.text("Nutze /vote <1-5> um fuer einen Build abzustimmen!", NamedTextColor.YELLOW));
        state = GameState.VOTING;
        startCountdown(votingTime, this::endGame);
    }

    public void endGame() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        state = GameState.ENDED;

        teams.sort((a, b) -> b.getScore() - a.getScore());

        broadcastAll(Component.text("=== SPIEL BEENDET ===", NamedTextColor.GOLD));
        broadcastAll(Component.text("Ergebnisse:", NamedTextColor.WHITE));

        int rank = 1;
        for (Team team : teams) {
            String playerNames = getPlayerNames(team);
            broadcastAll(Component.text(rank + ". " + team.getName() + " (" + playerNames + "): " + team.getScore() + " Punkte", NamedTextColor.AQUA));
            rank++;
        }

        for (Team team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }
    }

    public void stopGame() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        wallManager.removeOuterWalls(world, centerX, centerZ, areaSize * 4);
        wallManager.removeInnerWalls(world, centerX, centerZ, areaSize * 4);

        for (Team team : teams) {
            for (UUID uuid : team.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }

        state = GameState.WAITING;
        teams.clear();
        broadcastAll(Component.text("Das Spiel wurde abgebrochen.", NamedTextColor.RED));
    }

    public boolean submitVote(Player voter, int points) {
        if (state != GameState.VOTING) return false;

        Team voterTeam = getTeamOfPlayer(voter.getUniqueId());
        Team targetTeam = areaManager.getTeamAtLocation(voter.getLocation(), teams);

        if (targetTeam == null) {
            voter.sendMessage(Component.text("Stehe in einem Build-Bereich, um abzustimmen!", NamedTextColor.RED));
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
        team.addPlayer(player);
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
            Player p = Bukkit.getOfflinePlayer(uuid).getPlayer();
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
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
        if (min > 0) return min + "m " + sec + "s";
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
