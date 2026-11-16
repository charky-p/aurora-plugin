package org.charky.aurora.features.tool;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.ToolStore;
import org.charky.aurora.features.player.BackpackData;
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
        put(uuid, realm, tool, stat, level);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) refresh(player);
    }

    // Stores a level without redrawing anything
    private void put(UUID uuid, String realm, String tool, String stat, int level) {
        levels.computeIfAbsent(uuid, k -> new HashMap<>())
                .computeIfAbsent(realm, k -> new HashMap<>())
                .computeIfAbsent(tool, k -> new HashMap<>())
                .put(stat, level);
        db.async(() -> store.save(uuid, realm, tool, stat, level));
    }

    // Every starter tool the player does not carry. Armour goes on if the slot is free
    public void giveStarters(Player player) {
        int given = 0;
        for (ToolType tool : registry.getStarterTools()) {
            if (carries(player, tool)) continue;

            EquipmentSlot slot = registry.createTool(tool, Map.of(), null).getType().getEquipmentSlot();
            if (slot.isArmor() && player.getInventory().getItem(slot).getType().isAir()) {
                Realm realm = realmOf(player);
                if (realm != null && !has(player, tool)) put(player.getUniqueId(), realm.id(), tool.id(), ToolLevels.MATERIAL, 0);
                player.getInventory().setItem(slot, build(player, tool));
                given++;
            } else if (give(player, tool)) {
                given++;
            }
        }
        if (given == 0) TextUtils.send(player, "<gray>You already carry all of your tools.");
        else player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1f);
    }

    // Gear from before tools lived on the player, the item tiers the shops sold:
    // cobble_pick_3, gold_helmet_2, city_shield_1. Its enchants and material become
    // the player's levels, if they are higher than what they have, and the item is
    // swapped for the tool. Mob drops are named after their mob and are left alone
    private void migrate(Player player) {
        PlayerInventory inventory = player.getInventory();
        UUID uuid = player.getUniqueId();
        boolean changed = false;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            String id = DataRegistry.itemId(item);
            if (id == null || toolId(item) != null) continue;

            String[] parts = id.split("_");
            if (parts.length != 3 || !parts[2].chars().allMatch(Character::isDigit)) continue;
            ToolType tool = registry.getToolType(parts[1]);
            if (tool == null) continue;

            UpgradeZone zone = registry.getUpgradeZone(parts[0]);
            String realm = zone != null ? zone.realm() : parts[0].equals("starter") ? registry.getStarterRealm() : null;
            if (realm == null) continue;

            for (String stat : registry.getEnchantIds()) {
                int level = Enchants.level(item, stat);
                if (level > level(uuid, realm, tool.id(), stat)) put(uuid, realm, tool.id(), stat, level);
            }
            String type = item.getType().name();
            String suffix = "_" + tool.item();
            int material = type.endsWith(suffix)
                    ? ToolLevels.materialLevel(tool.materials(), type.substring(0, type.length() - suffix.length())) : 0;
            if (material > level(uuid, realm, tool.id(), ToolLevels.MATERIAL) || !own(uuid, realm, tool.id()).containsKey(ToolLevels.MATERIAL)) {
                put(uuid, realm, tool.id(), ToolLevels.MATERIAL, Math.max(0, material));
            }

            // The tool takes the old item's place, refresh below draws it properly
            inventory.setItem(slot, registry.createTool(tool, Map.of(), null));
            changed = true;
        }

        if (!changed) return;
        // Two old picks became two tools, one is enough
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            String tool = toolId(inventory.getItem(slot));
            if (tool != null && !seen.add(tool)) inventory.setItem(slot, null);
        }
        TextUtils.send(player, "<aqua>Your gear is now upgraded in place. <gray>Shops sell levels for the tools you carry.");
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

    public boolean carries(Player player, ToolType tool) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (tool.id().equals(toolId(item))) return true;
        }
        return false;
    }

    // ----------------------------------------------------------------- upgrades

    // Null while the backpack is still loading
    public BackpackData backpackOf(Player player) {
        return plugin.getBackpackManager().get(player.getUniqueId());
    }

    // A zone's steps count against what the player has in the zone's own realm
    private int own(Player player, UpgradeZone zone, String tool, String stat) {
        return level(player.getUniqueId(), zone.realm(), tool, stat);
    }

    public UpgradeZone.Step next(Player player, UpgradeZone zone, String tool) {
        return zone.next(tool, stat -> own(player, zone, tool, stat));
    }

    public UpgradeZone.Status status(Player player, UpgradeZone zone, String tool) {
        return zone.status(tool, stat -> own(player, zone, tool, stat));
    }

    // "Fortune 3", or "Iron Pickaxe" for a material
    public Component describe(ToolType tool, UpgradeZone.Step step) {
        if (step.stat().equals(ToolLevels.MATERIAL)) {
            String material = ToolLevels.material(tool.materials(), step.level());
            return Component.text(material.charAt(0) + material.substring(1).toLowerCase() + " " + tool.name());
        }
        return TextUtils.parse(registry.getEnchantDisplay(step.stat()) + " " + step.level());
    }

    // Null if the next step of the tool's chain was bought, otherwise why not
    public String buy(Player player, UpgradeZone zone, String toolId) {
        ToolType tool = registry.getToolType(toolId);
        BackpackData backpack = backpackOf(player);
        if (tool == null || backpack == null) return "Not ready yet, try again in a moment";

        UpgradeZone.Step step = next(player, zone, toolId);
        if (step == null) return "That tool is maxed here";
        if (status(player, zone, toolId) == UpgradeZone.Status.BEHIND) {
            return "Finish that tool's upgrades in the zone before first";
        }
        if (!backpack.hasItems(player, step.costs())) return "Not enough items";

        backpack.removeItems(player, step.costs());
        set(player.getUniqueId(), zone.realm(), toolId, step.stat(), step.level());

        player.sendMessage(TextUtils.parse("<gold><b>UPGRADED</b> <gray>" + tool.name() + ": <white>")
                .append(describe(tool, step)));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.4f);
        return null;
    }

    public void openUpgrades(Player player, String zoneId) {
        UpgradeZone zone = registry.getUpgradeZone(zoneId);
        if (zone == null) {
            TextUtils.error(player, "There are no upgrades here yet.");
            return;
        }
        new UpgradeMenu(this, registry, zone).open(player);
    }

    // Null for anything that is not one of these tools
    public static String toolId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(DataKeys.TOOL_KEY, PersistentDataType.STRING);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        migrate(event.getPlayer());
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
