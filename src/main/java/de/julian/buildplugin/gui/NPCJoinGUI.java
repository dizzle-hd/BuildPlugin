package de.julian.buildplugin.gui;

import de.julian.buildplugin.game.Game;
import de.julian.buildplugin.game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class NPCJoinGUI {

    public static final String GUI_TITLE_PREFIX = "BuildPlugin Join: ";

    private final Game game;

    public NPCJoinGUI(Game game) {
        this.game = game;
    }

    public void open(Player player, int buildTimeMinutes) {
        String title = GUI_TITLE_PREFIX + buildTimeMinutes + " Minuten";
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(title));

        // Solo button (left)
        inv.setItem(11, createItem(
                Material.PLAYER_HEAD,
                "Solo spielen",
                List.of(
                        "Bauzeit: " + buildTimeMinutes + " Minuten",
                        "Du baust alleine in deiner Parzelle.",
                        "",
                        "Klicken zum Beitreten!"
                ),
                NamedTextColor.YELLOW
        ));

        // Team button (right)
        inv.setItem(15, createItem(
                Material.LIME_WOOL,
                "Team spielen (2 Spieler)",
                List.of(
                        "Bauzeit: " + buildTimeMinutes + " Minuten",
                        "Baut zu zweit in eurer Parzelle.",
                        "",
                        "Klicken zum Beitreten!"
                ),
                NamedTextColor.GREEN
        ));

        // Info item in center
        inv.setItem(13, createItem(
                Material.CLOCK,
                "Bauzeit: " + buildTimeMinutes + " Minuten",
                List.of("Waehle deinen Spielmodus:"),
                NamedTextColor.AQUA
        ));

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, List<String> lore, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        meta.lore(lore.stream()
                .map(l -> l.isEmpty()
                        ? Component.empty()
                        : Component.text(l, NamedTextColor.GRAY))
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isNPCJoinTitle(String title) {
        return title.startsWith(GUI_TITLE_PREFIX);
    }

    public int extractMinutes(String title) {
        try {
            String part = title.replace(GUI_TITLE_PREFIX, "").replace(" Minuten", "").trim();
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
