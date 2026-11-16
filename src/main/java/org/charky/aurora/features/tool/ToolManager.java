package org.charky.aurora.features.tool;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.ToolStore;
import org.charky.aurora.features.tool.Realms.Realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Tools that live on the player. A tool's stats are levels kept per player and per
// realm, see ToolLevels. The item in the inventory is drawn from those levels and
// redrawn whenever they change or the player moves to another realm, so the same
// pick is one thing in the overworld and another in a mini world. Everything that
// reads an enchant still reads it off the item, which always shows the levels of
// whoever holds it, where they are: an item passed to someone else becomes theirs
public class ToolManager implements Listener {
    private final Aurora plugin;
    private final DataRegistry registry;
    private final Database db;
    private final ToolStore store;
    // uuid -> realm -> tool -> stat -> level
    private final Map<UUID, Map<String, Map<String, Map<String, Integer>>>> levels;

    public ToolManager(Aurora plugin, Database db, ToolStore store) {
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
        this.db = db;
        this.store = store;
        this.levels = store.loadAll();
    }

    // The realm a player is in, null only when realms.csv is empty
    public Realm realmOf(Player player) {
        return registry.getRealms().ofWorld(player.getWorld().getName());
    }

    // What a player has bought in one realm alone
    public int level(UUID uuid, String realm, String tool, String stat) {
        return own(uuid, realm, tool).getOrDefault(stat, 0);
    }

    private Map<String, Integer> own(UUID uuid, String realm, String tool) {
        return levels.getOrDefault(uuid, Map.of()).getOrDefault(realm, Map.of()).getOrDefault(tool, Map.of());
    }

    // What the tool has in a realm, with every realm that one builds on
    public Map<String, Integer> effective(UUID uuid, Realm realm, String tool) {
        List<Map<String, Integer>> chain = new ArrayList<>();
        for (Realm link : registry.getRealms().chain(realm)) chain.add(own(uuid, link.id(), tool));
        return ToolLevels.combine(chain);
    }

    // A tool the player has anything in, here or in a realm this one builds on
    public boolean has(Player player, ToolType tool) {
        Realm realm = realmOf(player);
        if (realm == null) return false;
        for (Realm link : registry.getRealms().chain(realm)) {
            if (levels.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(link.id(), Map.of())
                    .containsKey(tool.id())) return true;
        }
        return false;
    }

    public void set(UUID uuid, String realm, String tool, String stat, int level) {
        levels.computeIfAbsent(uuid, k -> new HashMap<>())
                .computeIfAbsent(realm, k -> new HashMap<>())
                .computeIfAbsent(tool, k -> new HashMap<>())
                .put(stat, level);
        db.async(() -> store.save(uuid, realm, tool, stat, level));

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) refresh(player);
    }

    // The tool as this player has it where they stand
    public ItemStack build(Player player, ToolType tool) {
        Realm realm = realmOf(player);
        Map<String, Integer> stats = realm == null ? Map.of() : effective(player.getUniqueId(), realm, tool.id());
        return registry.createTool(tool, stats, realm);
    }

    // Hands the tool over, false if they carry it already or have no room
    public boolean give(Player player, ToolType tool) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (tool.id().equals(toolId(item))) return false;
        }
        if (inventory.firstEmpty() == -1) return false;

        // Marks the tool as theirs, so the Armory lists it from now on
        Realm realm = realmOf(player);
        if (realm != null && !has(player, tool)) set(player.getUniqueId(), realm.id(), tool.id(), ToolLevels.MATERIAL, 0);

        inventory.addItem(build(player, tool));
        return true;
    }

    // Redraws every tool the player carries, armour and off hand included
    public void refresh(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ToolType tool = registry.getToolType(toolId(inventory.getItem(slot)));
            if (tool != null) inventory.setItem(slot, build(player, tool));
        }
    }

    // Null for anything that is not one of these tools
    public static String toolId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(DataKeys.TOOL_KEY, PersistentDataType.STRING);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        refresh(event.getPlayer());
    }

    // Next tick, during the event the player is not in the world they respawn in
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) refresh(player);
        });
    }
}
