package org.charky.aurora.features.system;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The two corners each admin has selected with the wand
public class WandManager {
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    // Marks an item as the wand, shared by the command that gives it and the
    // listener that reads it
    public static NamespacedKey wandKey(Plugin plugin) {
        return new NamespacedKey(plugin, "is_wand");
    }

    public void setPos1(UUID uuid, Location loc) { pos1.put(uuid, loc); }
    public void setPos2(UUID uuid, Location loc) { pos2.put(uuid, loc); }

    public Location getPos1(UUID uuid) { return pos1.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2.get(uuid); }

    public boolean hasBothPositions(UUID uuid) {
        return pos1.containsKey(uuid) && pos2.containsKey(uuid);
    }
}
