package org.charky.aurora.features.tool;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.data.ItemFactory;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.ToolStore;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.tool.Realms.Realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    // The armour trim a player chose in /customise, null for none. Looks only
    private Function<Player, ArmorTrim> trims = player -> null;
    // Told when a player buys the last upgrade a zone sells
    private ZoneMaxed onMaxed = (player, zoneId, region) -> {};
    // Told when a player opens a zone's upgrades, and when they buy one. The tutorial listens
    private BiConsumer<Player, String> onOpened = (player, zoneId) -> {};
    private BiConsumer<Player, String> onBought = (player, zoneId) -> {};

    public interface ZoneMaxed {
        void accept(Player player, String zoneId, String region);
    }

    public void setTrims(Function<Player, ArmorTrim> trims) {
        this.trims = trims;
    }

    public void setOnMaxed(ZoneMaxed onMaxed) {
        this.onMaxed = onMaxed;
    }

    public void setOnOpened(BiConsumer<Player, String> onOpened) {
        this.onOpened = onOpened;
    }

    public void setOnBought(BiConsumer<Player, String> onBought) {
        this.onBought = onBought;
    }

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

    // Every level in every realm goes, and the player is handed plain starter tools
    public void reset(Player player) {
        UUID uuid = player.getUniqueId();
        levels.remove(uuid);
        db.async(() -> store.deleteAll(uuid));

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (toolId(inventory.getItem(slot)) != null) inventory.setItem(slot, null);
        }
        giveStarters(player);
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

    // The tool as this player has it where they stand
    public ItemStack build(Player player, ToolType tool) {
        Realm realm = realmOf(player);
        Map<String, Integer> stats = realm == null ? Map.of() : effective(player.getUniqueId(), realm, tool.id());
        ItemStack item = registry.createTool(tool, stats, realm);

        // Only armour has an ArmorMeta, so this leaves every other tool alone
        ArmorTrim trim = trims.apply(player);
        if (trim != null) {
            item.editMeta(ArmorMeta.class, meta -> {
                meta.setTrim(trim);
                meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
            });
        }
        return item;
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
            String id = toolId(inventory.getItem(slot));
            if (OLD_GOD_TOOL.equals(id)) {
                inventory.setItem(slot, null);
                continue;
            }
            // The grapple was once a shop item, one still carried becomes the tool, and theirs
            if (id == null && OLD_GRAPPLE.equals(DataRegistry.itemId(inventory.getItem(slot)))) {
                ToolType grapple = registry.getToolType(OLD_GRAPPLE);
                Realm realm = realmOf(player);
                if (grapple == null || realm == null) continue;
                if (!owns(player.getUniqueId(), OLD_GRAPPLE)) put(player.getUniqueId(), realm.id(), OLD_GRAPPLE, ToolLevels.MATERIAL, 0);
                inventory.setItem(slot, build(player, grapple));
                continue;
            }
            ToolType tool = registry.getToolType(id);
            if (tool != null) inventory.setItem(slot, build(player, tool));
        }
    }

    // The God Tool was taken out of the game, one still carried is removed when tools are redrawn
    private static final String OLD_GOD_TOOL = "god";
    // The grapple's item id from when it was sold as an item, and its tool id now
    private static final String OLD_GRAPPLE = "grapple";

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

    // A zone's steps count against what the player has in the zone's own realm. The
    // unlock is 1 once the tool is theirs, in any realm
    private int own(Player player, UpgradeZone zone, String tool, String stat) {
        if (stat.equals(ToolLevels.UNLOCK)) return owns(player.getUniqueId(), tool) ? 1 : 0;
        return level(player.getUniqueId(), zone.realm(), tool, stat);
    }

    // Bought or handed out once, in any realm. Owning a tool is having levels for it
    public boolean owns(UUID uuid, String tool) {
        for (Map<String, Map<String, Integer>> realm : levels.getOrDefault(uuid, Map.of()).values()) {
            if (realm.containsKey(tool)) return true;
        }
        return false;
    }

    public UpgradeZone.Step next(Player player, UpgradeZone zone, String tool) {
        return zone.next(tool, stat -> own(player, zone, tool, stat));
    }

    // Behind while a zone played before this one still sells the tool something
    public UpgradeZone.Status status(Player player, UpgradeZone zone, String tool) {
        UpgradeZone.Status status = zone.status(tool, stat -> own(player, zone, tool, stat));
        return status == UpgradeZone.Status.OPEN && unfinished(player, zone, tool) != null ? UpgradeZone.Status.BEHIND : status;
    }

    // The first zone before this one with a step of the tool not bought yet, null when
    // there is none. A released zone never waits on one not out yet, players cannot buy
    // there. In a zone not out yet, which only admins reach, every zone before it counts
    public UpgradeZone unfinished(Player player, UpgradeZone zone, String tool) {
        boolean released = registry.isZoneReleased(zone.id());
        for (String id : zone.before()) {
            UpgradeZone earlier = registry.getUpgradeZone(id);
            if (earlier == null || (released && !registry.isZoneReleased(id))) continue;
            if (next(player, earlier, tool) != null) return earlier;
        }
        return null;
    }

    // Why a tool is behind, for the shop to say. Only behind while an earlier zone is
    // unfinished, so there is always one to name
    public String behindReason(Player player, UpgradeZone zone, ToolType tool) {
        UpgradeZone earlier = unfinished(player, zone, tool.id());
        return "Buy every " + tool.name() + " upgrade at " + (earlier == null ? "the zones before" : earlier.name()) + " first";
    }

    // How many steps of a tool's chain in a zone are bought
    public int bought(Player player, UpgradeZone zone, String tool) {
        int bought = 0;
        for (UpgradeZone.Step step : zone.chain(tool)) {
            if (own(player, zone, tool, step.stat()) >= step.level()) bought++;
        }
        return bought;
    }

    // Every upgrade the zone sells is bought
    public boolean maxed(Player player, String zoneId) {
        UpgradeZone zone = registry.getUpgradeZone(zoneId);
        if (zone == null || zone.tools().isEmpty()) return false;
        for (String tool : zone.tools()) {
            if (status(player, zone, tool) != UpgradeZone.Status.MAXED) return false;
        }
        return true;
    }

    // Every zone in the game is maxed, what the Overworld Core needs
    // Every released zone, the ones not out yet cannot be bought
    public boolean maxedAll(Player player) {
        for (String zone : registry.getReleasedZoneIds()) {
            if (!maxed(player, zone)) return false;
        }
        return true;
    }

    // Every zone of a world is maxed. False for a world with no zones yet
    public boolean maxedRegion(Player player, String region) {
        List<String> zones = registry.getZonesOfRegion(region);
        if (zones.isEmpty()) return false;
        for (String zone : zones) {
            if (!maxed(player, zone)) return false;
        }
        return true;
    }

    // "Fortune 8", or "Iron Pickaxe" for a material. The number is what the tool
    // will have in all once the step is bought, not the level within the zone's
    // realm, since realms add up
    public Component describe(Player player, UpgradeZone zone, ToolType tool, UpgradeZone.Step step) {
        if (step.stat().equals(ToolLevels.MATERIAL) || step.stat().equals(ToolLevels.UNLOCK)) {
            String material = ToolLevels.material(tool.materials(), step.stat().equals(ToolLevels.UNLOCK) ? 0 : step.level());
            if (material.equalsIgnoreCase(ToolLevels.NO_MATERIAL)) return Component.text(tool.name());
            return Component.text(material.charAt(0) + material.substring(1).toLowerCase() + " " + tool.name());
        }
        Realm realm = realmOf(player);
        int total = realm == null ? 0 : effective(player.getUniqueId(), realm, tool.id()).getOrDefault(step.stat(), 0);
        int after = total - own(player, zone, tool.id(), step.stat()) + step.level();
        return TextUtils.parse(registry.getEnchantDisplay(step.stat()) + " " + after);
    }

    // Null if the next step of the tool's chain was bought, otherwise why not
    public String buy(Player player, UpgradeZone zone, String toolId) {
        ToolType tool = registry.getToolType(toolId);
        BackpackData backpack = backpackOf(player);
        if (tool == null || backpack == null) return "Not ready yet, try again in a moment";

        UpgradeZone.Step step = next(player, zone, toolId);
        if (step == null) return "That tool is maxed here";
        if (status(player, zone, toolId) == UpgradeZone.Status.BEHIND) return behindReason(player, zone, tool);
        boolean unlock = step.stat().equals(ToolLevels.UNLOCK);
        if (!unlock && !owns(player.getUniqueId(), toolId)) return "You need to buy the " + tool.name() + " first";
        if (!backpack.hasItems(player, step.costs())) return "Not enough items";

        backpack.removeItems(player, step.costs());
        Component bought = describe(player, zone, tool, step);
        // Bought before it is handed over, so a full inventory never costs the tool
        if (unlock) set(player.getUniqueId(), zone.realm(), toolId, ToolLevels.MATERIAL, 0);
        else set(player.getUniqueId(), zone.realm(), toolId, step.stat(), step.level());
        if (!carries(player, tool) && !give(player, tool)) TextUtils.error(player, "Your inventory is full, click the trader again to take it.");

        player.sendMessage(TextUtils.parse("<gold><b>UPGRADED</b> <gray>" + tool.name() + ": <white>").append(bought));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.4f);
        if (maxed(player, zone.id())) onMaxed.accept(player, zone.id(), registry.getZoneRegion(zone.id()));
        onBought.accept(player, zone.id());
        return null;
    }

    public void openUpgrades(Player player, String zoneId) {
        UpgradeZone zone = registry.getUpgradeZone(zoneId);
        if (zone == null) {
            TextUtils.error(player, "There are no upgrades here yet.");
            return;
        }
        new UpgradeMenu(this, registry, zone).open(player);
        onOpened.accept(player, zoneId);
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
