package org.charky.aurora.features.gauntlet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.GauntletStore;
import org.charky.aurora.features.combat.BowListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

// A player's gauntlet setup: their gear, the starting kit they have bought (a totem,
// potions, arrows ...) and the Gauntlet menu item, laid out in the inventory the way
// they like. In the lobby the inventory is the setup: whatever they move around there
// is remembered. A run starts with it, and it comes back whole after a run, a death,
// or walking out of the PvP zone, whatever was used up or picked up meanwhile
public class GauntletSetup implements Listener {
    private static final NamespacedKey SETUP_KEY = new NamespacedKey("aurora", "gauntlet_setup");
    private static final NamespacedKey MENU_KEY = new NamespacedKey("aurora", "gauntlet_menu");
    private static final String GEAR = "gear:";
    private static final String SETUP = "setup:";
    private static final String MENU = "menu";
    private static final int MENU_SLOT = 8;

    private final Aurora plugin;
    private final Database db;
    private final GauntletStore store;
    private final GauntletManager manager;
    // Standing in the PvP zone, to put the setup back when they walk out
    private final Set<UUID> inPvp = new HashSet<>();
    // A layout save already waiting for the next tick
    private final Set<UUID> saving = new HashSet<>();

    public GauntletSetup(Aurora plugin, Database db, GauntletStore store, GauntletManager manager) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.manager = manager;
    }

    // ---- The items ----

    // Every item of the setup by its key, in the forge's order
    private Map<String, ItemStack> items(Player player) {
        GauntletProfile profile = manager.profile(player);
        GauntletData data = manager.data();
        Map<String, ItemStack> items = new LinkedHashMap<>();
        for (GauntletData.Gear gear : data.gear().values()) {
            GauntletData.GearLevel level = data.level(profile.level(gear.id()));
            if (profile.owns(gear.id()) && level != null) items.put(GEAR + gear.id(), GauntletGear.build(manager.registry(), gear, level));
        }
        for (GauntletData.SetupItem setup : data.setup().values()) {
            int level = profile.setup.getOrDefault(setup.id(), 0);
            if (level <= 0) continue;
            ItemStack item = GauntletCarePackage.lootItem(setup.item(), Math.min(setup.amount() * level, 64));
            if (item == null) continue;
            item.editMeta(meta -> {
                // The elytra never wears out
                meta.setUnbreakable(true);
                meta.getPersistentDataContainer().set(SETUP_KEY, PersistentDataType.STRING, setup.id());
            });
            items.put(SETUP + setup.id(), item);
        }
        items.put(MENU, menuItem());
        return items;
    }

    private static ItemStack menuItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic><red><b>Gauntlet Menu <gray>(right click)"));
            meta.lore(List.of(TextUtils.parse("<!italic><gray>Start a run, the forge, your starting kit,"),
                    TextUtils.parse("<!italic><gray>milestones and the top floors")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(MENU_KEY, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    // gear:sword, setup:totem or menu; null for anything else
    private static String keyOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String gear = pdc.get(BowListener.GAUNTLET_KEY, PersistentDataType.STRING);
        if (gear != null) return GEAR + gear;
        String setup = pdc.get(SETUP_KEY, PersistentDataType.STRING);
        if (setup != null) return SETUP + setup;
        return pdc.has(MENU_KEY, PersistentDataType.BYTE) ? MENU : null;
    }

    static boolean isMenuItem(ItemStack item) {
        return MENU.equals(keyOf(item));
    }

    // ---- Putting it back ----

    // Empties the inventory and lays the setup out as the player last left it in the
    // lobby. Anything new goes where it belongs: armour on, the shield in the off
    // hand, the menu in the last hotbar slot, the rest in the first free slot
    public void restore(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        player.setItemOnCursor(null);
        Map<String, ItemStack> items = items(player);
        Map<Integer, String> layout = manager.profile(player).layout;

        for (Map.Entry<Integer, String> entry : new TreeMap<>(layout).entrySet()) {
            ItemStack item = items.remove(entry.getValue());
            if (item != null && entry.getKey() >= 0 && entry.getKey() < inventory.getSize()) inventory.setItem(entry.getKey(), item);
        }
        for (Map.Entry<String, ItemStack> entry : items.entrySet()) {
            ItemStack item = entry.getValue();
            EquipmentSlot slot = item.getType().getEquipmentSlot();
            // The elytra waits in the inventory, the chestplate is worn
            if (slot.isArmor() && item.getType() != Material.ELYTRA && isEmpty(inventory.getItem(slot))) inventory.setItem(slot, item);
            else if (item.getType() == Material.SHIELD && isEmpty(inventory.getItemInOffHand())) inventory.setItemInOffHand(item);
            else if (entry.getKey().equals(MENU) && isEmpty(inventory.getItem(MENU_SLOT))) inventory.setItem(MENU_SLOT, item);
            else inventory.addItem(item);
        }
        player.updateInventory();
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    // In the lobby, out of the PvP zone and out of a run, the inventory is the setup
    private boolean editing(Player player) {
        return manager.isHere(player) && !manager.inRun(player) && !manager.inDuel(player) && !manager.isWatching(player)
                && !manager.arena().inPvp(player.getLocation());
    }

    // Where each setup item sits now, saved as the layout
    private void saveLayout(Player player) {
        if (!editing(player)) return;
        PlayerInventory inventory = player.getInventory();
        Map<Integer, String> layout = new HashMap<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            String key = keyOf(inventory.getItem(slot));
            if (key != null) layout.put(slot, key);
        }
        GauntletProfile profile = manager.profile(player);
        if (layout.equals(profile.layout)) return;
        profile.layout.clear();
        profile.layout.putAll(layout);
        UUID uuid = player.getUniqueId();
        db.async(() -> store.saveLayout(uuid, layout));
    }

    // Once the click has gone through
    private void saveLater(Player player) {
        if (!editing(player) || !saving.add(player.getUniqueId())) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            saving.remove(player.getUniqueId());
            if (player.isOnline()) saveLayout(player);
        });
    }

    // Something of the setup used up in the lobby comes straight back
    private void restoreLater(Player player) {
        if (!editing(player)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && editing(player)) restore(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) saveLater(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) saveLater(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        saveLater(event.getPlayer());
    }

    // Gear and the menu item are never dropped in the Gauntlet's world; in the lobby
    // nothing of the setup is
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!manager.isHere(player)) return;
        String key = keyOf(event.getItemDrop().getItemStack());
        if (key == null) return;
        if (editing(player) || key.startsWith(GEAR) || key.equals(MENU)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        restoreLater(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onThrow(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) restoreLater(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) restoreLater(player);
    }

    // The menu item opens the Gauntlet's menu
    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isMenuItem(event.getItem())) return;
        event.setCancelled(true);
        manager.openMenu(event.getPlayer());
    }

    // ---- The PvP zone ----

    // Last, after GauntletPvp has held anyone walking out of the zone back in it
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        checkPvp(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        checkPvp(event.getPlayer(), event.getTo());
    }

    // Leaving the PvP zone (by /spawn's countdown, or dying) puts the setup back as it was in the lobby
    private void checkPvp(Player player, Location to) {
        if (!manager.isHere(player) || manager.inRun(player)) {
            inPvp.remove(player.getUniqueId());
            return;
        }
        boolean now = manager.arena().inPvp(to);
        boolean was = now ? !inPvp.add(player.getUniqueId()) : inPvp.remove(player.getUniqueId());
        if (was && !now) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                restore(player);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.6f, 1.2f);
                player.sendActionBar(TextUtils.parse("<gray>Your setup is back"));
            });
        } else if (now && !was) {
            player.sendActionBar(TextUtils.parse("<red><b>PvP ZONE</b> <gray>leave it to get your setup back"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inPvp.remove(event.getPlayer().getUniqueId());
        saving.remove(event.getPlayer().getUniqueId());
    }

    // ---- The starting kit shop ----

    // Null if it worked, otherwise why not
    public String buy(Player player, String setupId) {
        GauntletData.SetupItem setup = manager.data().setup().get(setupId);
        if (setup == null) return "There is no such item";
        if (manager.inRun(player)) return "The starting kit can't be changed during a run";
        GauntletProfile profile = manager.profile(player);
        int level = profile.setup.getOrDefault(setupId, 0);
        if (level >= setup.max()) return "You already start with the most of it";
        int next = level + 1;
        String unpaid = profile.pay(setup.shardsFor(next), setup.coresFor(next));
        if (unpaid != null) return unpaid;
        profile.setup.put(setupId, next);
        UUID uuid = player.getUniqueId();
        db.async(() -> store.saveSetup(uuid, setupId, next));
        manager.save(uuid);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.2f);
        TextUtils.send(player, "<green>You now start every run with <white>" + Math.min(setup.amount() * next, 64) + " " + setup.name());
        if (editing(player)) restore(player);
        manager.refreshBoard(player);
        return null;
    }
}
