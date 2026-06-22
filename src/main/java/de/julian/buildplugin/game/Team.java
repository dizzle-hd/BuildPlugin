package de.julian.buildplugin.game;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Team {

    private final String name;
    private final List<UUID> players = new ArrayList<>();
    private BuildArea area;
    private int score = 0;
    private final Map<UUID, Integer> receivedVotes = new HashMap<>();

    public Team(String name) {
        this.name = name;
    }

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public void addVote(UUID voter, int points) {
        receivedVotes.put(voter, points);
        score += points;
    }

    public boolean hasVotedFrom(UUID voter) {
        return receivedVotes.containsKey(voter);
    }

    public String getName() { return name; }
    public List<UUID> getPlayers() { return players; }
    public BuildArea getArea() { return area; }
    public void setArea(BuildArea area) { this.area = area; }
    public int getScore() { return score; }
}
