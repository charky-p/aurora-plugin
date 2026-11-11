package org.charky.aurora.core.data;

import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Reads one generated YAML file into its model class, keyed by id
public class YamlLoader {
    private final JavaPlugin plugin;

    public YamlLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Entries keep the file's order, which is the CSV's, so progression order
    // survives into the backpack and Armory
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> load(String fileName, Class<T> modelClass) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, false);

        Yaml yaml = new Yaml();
        try (InputStream in = new FileInputStream(file)) {
            Map<String, Object> root = yaml.load(in);
            if (root == null || !root.containsKey("items")) return Collections.emptyMap();

            Map<String, Object> entries = (Map<String, Object>) root.get("items");
            Map<String, T> result = new LinkedHashMap<>();

            Yaml modelMapper = new Yaml(new Constructor(modelClass, new LoaderOptions()));
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                result.put(entry.getKey(), modelMapper.load(yaml.dump(entry.getValue())));
            }
            return result;
        } catch (Exception e) {
            // Any malformed file, one bad file should not stop the rest loading
            plugin.getLogger().severe("Failed to load " + fileName + ": " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
