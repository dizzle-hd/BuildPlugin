package de.julian.buildplugin.gui;

import de.julian.buildplugin.game.Game;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SettingsGUI {

    public static final String GUI_TITLE = "BuildPlugin Einstellungen";

    private final Game game;

    public SettingsGUI(Game game) {
        this.game = game;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(GUI_TITLE));

        // Build time buttons (row 1)
        inv.setItem(10, buildTimeButton(30 * 60, "30 Minuten"));
        inv.setItem(11, buildTimeButton(45 * 60, "45 Minuten"));
        inv.setItem(12, buildTimeButton(60 * 60, "60 Minuten"));
        inv.setItem(13, buildTimeButton(90 * 60, "90 Minuten"));
        inv.setItem(14, buildTimeButton(120 * 60, "120 Minuten"));

        // Voting time buttons (row 2)
        inv.setItem(19, votingTimeButton(5 * 60, "5 Minuten"));
        inv.setItem(20, votingTimeButton(10 * 60, "10 Minuten"));
        inv.setItem(21, votingTimeButton(15 * 60, "15 Minuten"));

        // Mode toggle (row 3)
        inv.setItem(28, createItem(
                game.isSoloMode() ? Material.PLAYER_HEAD : Material.LIME_WOOL,
                game.isSoloMode() ? "Modus: Solo" : "Modus: Teams (2x2)",
                List.of("Klicken zum Wechseln")
        ));

        // Info display
        inv.setItem(4, createItem(Material.CLOCK,
                "Aktuelle Einstellungen",
                List.of(
                        "Bauzeit: " + formatTime(game.getBuildTime()),
                        "Abstimmzeit: " + formatTime(game.getVotingTime()),
                        "Modus: " + (game.isSoloMode() ? "Solo" : "Teams")
                )
        ));

        // Start button
        inv.setItem(49, createItem(Material.LIME_CONCRETE, "SPIEL STARTEN", List.of("Klicken um das Spiel zu starten")));

        // Stop/Reset button
        inv.setItem(45, createItem(Material.RED_CONCRETE, "SPIEL STOPPEN", List.of("Klicken um das Spiel abzubrechen")));

        player.openInventory(inv);
    }

    private ItemStack buildTimeButton(int seconds, String label) {
        boolean active = game.getBuildTime() == seconds;
        Material mat = active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        return createItem(mat, "Bauzeit: " + label, List.of(active ? "Aktuell ausgewaehlt" : "Klicken zum Auswaehlen"));
    }

    private ItemStack votingTimeButton(int seconds, String label) {
        boolean active = game.getVotingTime() == seconds;
        Material mat = active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        return createItem(mat, "Abstimmzeit: " + label, List.of(active ? "Aktuell ausgewaehlt" : "Klicken zum Auswaehlen"));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE));
        meta.lore(lore.stream()
                .map(l -> Component.text(l, NamedTextColor.GRAY))
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0 && sec > 0) return min + "m " + sec + "s";
        if (min > 0) return min + "m";
        return sec + "s";
    }
}
