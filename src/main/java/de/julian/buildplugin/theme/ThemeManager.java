package de.julian.buildplugin.theme;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Loads build themes from themes.yml and picks a random category + topic
 * at the start of each game.
 */
public class ThemeManager {

    private final Plugin plugin;
    private final Map<String, List<String>> categories = new LinkedHashMap<>();
    private final Random random = new Random();

    private String currentCategory;
    private String currentTopic;

    public ThemeManager(Plugin plugin) {
        this.plugin = plugin;
        loadThemes();
    }

    private void loadThemes() {
        categories.clear();

        File file = new File(plugin.getDataFolder(), "themes.yml");
        if (!file.exists()) {
            plugin.saveResource("themes.yml", false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("categories");
        if (section == null) {
            plugin.getLogger().warning("themes.yml has no 'categories' section!");
            return;
        }

        for (String key : section.getKeys(false)) {
            List<String> topics = section.getStringList(key);
            if (!topics.isEmpty()) {
                categories.put(key, topics);
            }
        }

        int totalTopics = categories.values().stream().mapToInt(List::size).sum();
        plugin.getLogger().info("Loaded " + categories.size() + " theme categories with " + totalTopics + " topics.");
    }

    public void reload() {
        loadThemes();
    }

    /**
     * Picks a random category, then a random topic from that category.
     */
    public void pickRandomTheme() {
        if (categories.isEmpty()) {
            currentCategory = "Frei";
            currentTopic = "Baue was du willst";
            return;
        }
        List<String> catNames = new ArrayList<>(categories.keySet());
        currentCategory = catNames.get(random.nextInt(catNames.size()));

        List<String> topics = categories.get(currentCategory);
        currentTopic = topics.get(random.nextInt(topics.size()));
    }

    public String getCurrentCategory() { return currentCategory; }
    public String getCurrentTopic() { return currentTopic; }

    public boolean hasTheme() {
        return currentCategory != null && currentTopic != null;
    }

    public int getCategoryCount() { return categories.size(); }
}
