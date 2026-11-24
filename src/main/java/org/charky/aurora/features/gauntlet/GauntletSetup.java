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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

// A player's gauntlet setup: their gear and the starting kit they have bought (a totem,
// potions, arrows ...), laid out in the Loadout (LoadoutMenu) the way they like. Runs,
// duels and the PvP zone hand out the setup alone, whole each time, whatever was used up
// or picked up before. The lobby holds only the Gauntlet menu item, a start item
// (straight into a run) and a leave item (out of the Gauntlet)
public class GauntletSetup implements Listener {
    private static final NamespacedKey SETUP_KEY = new NamespacedKey("aurora", "gauntlet_setup");
    private static final NamespacedKey MENU_KEY = new NamespacedKey("aurora", "gauntlet_menu");
    private static final NamespacedKey START_KEY = new NamespacedKey("aurora", "gauntlet_start");
    private static final NamespacedKey LEAVE_KEY = new NamespacedKey("aurora", "gauntlet_leave");
    private static final String GEAR = "gear:";
    private static final String SETUP = "setup:";
    private static final String MENU = "menu";
    private static final String START = "start";
    private static final String LEAVE = "leave";
    // Where the menu, start and leave items go
    private static final Map<String, Integer> TOOL_SLOTS = Map.of(MENU, 8, START, 7, LEAVE, 6);
    // The inventory's armour slots and its off hand; slots below STORAGE are the hotbar
    // (0-8) and the rows above it (9-35)
    private static final Map<EquipmentSlot, Integer> ARMOUR_SLOTS = Map.of(
            EquipmentSlot.FEET, 36, EquipmentSlot.LEGS, 37, EquipmentSlot.CHEST, 38, EquipmentSlot.HEAD, 39);
    static final int OFF_HAND = 40;
    static final int STORAGE = 36;

    // Where a player is, for what their inventory holds
    private enum Where { LOBBY, PVP, DUEL, RUN }

    private final Aurora plugin;
    private final Database db;
    private final GauntletStore store;
    private final GauntletManager manager;
    // Standing in the PvP zone, to put the setup away when they walk out
    private final Set<UUID> inPvp = new HashSet<>();

    public GauntletSetup(Aurora plugin, Database db, GauntletStore store, GauntletManager manager) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.manager = manager;
    }

    // ---- The items ----

    private Where where(Player player) {
        if (manager.inRun(player)) return Where.RUN;
        if (manager.inDuel(player)) return Where.DUEL;
        return manager.arena().inPvp(player.getLocation()) ? Where.PVP : Where.LOBBY;
    }

    // What the inventory holds where they are, by key: the setup in a fight, the menu,
    // start and leave items in the lobby
    private Map<String, ItemStack> items(Player player) {
        Where where = where(player);
        if (where != Where.LOBBY) return setup(player, where == Where.RUN);
        Map<String, ItemStack> items = new LinkedHashMap<>();
        items.put(MENU, tool(Material.NETHER_STAR, MENU_KEY, "<red><b>Gauntlet Menu <gray>(right click)",
                List.of("<gray>Start a run, the forge, your starting kit,", "<gray>your loadout, milestones and the top floors")));
        items.put(START, tool(Material.LIME_DYE, START_KEY, "<green><b>Start a Run <gray>(right click)",
                List.of("<gray>Straight into a run, on the floor", "<gray>and heat you chose last")));
        items.put(LEAVE, tool(Material.OAK_DOOR, LEAVE_KEY, "<gray><b>Leave the Gauntlet <gray>(right click)",
                List.of("<gray>Back to the main spawn,", "<gray>with your own inventory")));
        return items;
    }

    // The gear and the starting kit by key, in the forge's order. For a run the kit is
    // marked like the care packages' loot, so arrows and apples stack with theirs;
    // otherwise it is marked for the layout
    private Map<String, ItemStack> setup(Player player, boolean run) {
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
            ItemStack item = kitItem(setup, level);
            if (item == null) continue;
            if (run) GauntletGear.markRunItem(item);
            else item.editMeta(meta -> meta.getPersistentDataContainer().set(SETUP_KEY, PersistentDataType.STRING, setup.id()));
            // Nothing of it wears out (the elytra, the grapple)
            if (item.getType().getMaxDurability() > 0) item.editMeta(meta -> meta.setUnbreakable(true));
            items.put(SETUP + setup.id(), item);
        }
        return items;
    }

    // A kit item at a level: vanilla's, by the amount a level, or the main game's tool
    // (its upgrades rising with the level) or item. Null if there is no such thing
    ItemStack kitItem(GauntletData.SetupItem setup, int level) {
        if (setup.tool() != null) return GauntletGear.mainTool(manager.registry(), setup.tool(), setup.upgrades(level));
        if (setup.mainItem() != null) return GauntletGear.mainItem(manager.registry(), setup.mainItem());
        return GauntletCarePackage.lootItem(setup.item(), setup.count(level));
    }

    // Anything of the Gauntlet's own: gear, the starting kit, the menu, start and leave
    // items, a run's loot, the kit's main-game tools and PvP loot. None of it belongs
    // anywhere else, nor in an inventory put away for the main game
    static boolean isGauntletItem(ItemStack item) {
        return keyOf(item) != null || GauntletGear.isRunItem(item) || GauntletGear.isKit(item) || GauntletPvp.isLoot(item);
    }

    // The menu, start and leave items, which never leave their player
    static boolean isTool(ItemStack item) {
        String key = keyOf(item);
        return key != null && TOOL_SLOTS.containsKey(key);
    }

    // An item that does something when right clicked
    private static ItemStack tool(Material material, NamespacedKey key, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic>" + name));
            meta.lore(lore.stream().map(line -> TextUtils.parse("<!italic>" + line)).toList());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    // gear:sword, setup:totem, menu, start or leave; null for anything else
    private static String keyOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String gear = pdc.get(BowListener.GAUNTLET_KEY, PersistentDataType.STRING);
        if (gear != null) return GEAR + gear;
        String setup = pdc.get(SETUP_KEY, PersistentDataType.STRING);
        if (setup != null) return SETUP + setup;
        if (pdc.has(MENU_KEY, PersistentDataType.BYTE)) return MENU;
        if (pdc.has(START_KEY, PersistentDataType.BYTE)) return START;
        return pdc.has(LEAVE_KEY, PersistentDataType.BYTE) ? LEAVE : null;
    }

    // ---- Laying it out ----

    // Empties the inventory and fills it with what it holds where they are: the setup
    // as the Loadout lays it out, the lobby's menu, start and leave items always at the
    // end of the hotbar (a layout from before the Loadout could have put them anywhere)
    public void restore(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        player.setItemOnCursor(null);
        Map<Integer, String> layout = where(player) == Where.LOBBY ? Map.of() : manager.profile(player).layout;
        place(items(player), layout).forEach(inventory::setItem);
        player.updateInventory();
    }

    // Where each item goes, by inventory slot: where the layout has it; otherwise armour
    // worn (the elytra waits in the inventory), the shield in the off hand, the menu,
    // start and leave items at the end of the hotbar, and the rest in the first free
    // slots, the hotbar first
    static Map<Integer, ItemStack> place(Map<String, ItemStack> items, Map<Integer, String> layout) {
        Map<String, ItemStack> left = new LinkedHashMap<>(items);
        Map<Integer, ItemStack> placed = new HashMap<>();
        for (Map.Entry<Integer, String> entry : new TreeMap<>(layout).entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot > OFF_HAND || placed.containsKey(slot) || !left.containsKey(entry.getValue())) continue;
            placed.put(slot, left.remove(entry.getValue()));
        }
        List<ItemStack> rest = new ArrayList<>();
        for (Map.Entry<String, ItemStack> entry : left.entrySet()) {
            ItemStack item = entry.getValue();
            Material type = item.getType();
            Integer slot = TOOL_SLOTS.get(entry.getKey());
            if (type == Material.SHIELD) slot = OFF_HAND;
            else if (type != Material.ELYTRA && ARMOUR_SLOTS.containsKey(type.getEquipmentSlot())) slot = ARMOUR_SLOTS.get(type.getEquipmentSlot());
            if (slot != null && !placed.containsKey(slot)) placed.put(slot, item);
            else rest.add(item);
        }
        int free = 0;
        for (ItemStack item : rest) {
            while (free < STORAGE && placed.containsKey(free)) free++;
            if (free < STORAGE) placed.put(free, item);
        }
        return placed;
    }

    // ---- The Loadout ----

    // The Loadout, from /gauntlet loadout or the menu, in the lobby. Null if it opened,
    // otherwise why not
    public String openLoadout(Player player) {
        if (!manager.isHere(player) || where(player) != Where.LOBBY || manager.isWatching(player)) {
            return "Set your loadout in the Gauntlet's lobby";
        }
        player.openInventory(new LoadoutMenu(loadout(player, manager.profile(player).layout)).getInventory());
        return null;
    }

    // The setup laid out by a layout, as the Loadout shows it
    private Map<Integer, ItemStack> loadout(Player player, Map<Integer, String> layout) {
        return place(setup(player, false), layout);
    }

    // In the Loadout whole stacks move between its slots and nowhere else: no shift
    // clicks, number keys, drops, double clicks or half stacks, nothing taken out of it.
    // Reset lays everything out afresh
    @EventHandler(ignoreCancelled = true)
    public void onLoadoutClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof LoadoutMenu loadout)) return;
        int raw = event.getRawSlot();
        if (raw == LoadoutMenu.RESET_SLOT && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            if (player.getItemOnCursor().getType().isAir()) loadout.draw(loadout(player, Map.of()));
            return;
        }
        boolean moving = switch (event.getAction()) {
            case PICKUP_ALL, PLACE_ALL, SWAP_WITH_CURSOR, NOTHING -> true;
            default -> false;
        };
        if (!moving || raw >= LoadoutMenu.SIZE || LoadoutMenu.inventorySlot(raw) < 0) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoadoutDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof LoadoutMenu) event.setCancelled(true);
    }

    // Closing the Loadout saves it, a stack still held put in the first free slot
    @EventHandler
    public void onLoadoutClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof LoadoutMenu) || !(event.getPlayer() instanceof Player player)) return;
        Inventory chest = event.getInventory();
        ItemStack held = player.getItemOnCursor();
        player.setItemOnCursor(null);
        if (!held.getType().isAir()) {
            int free = chest.firstEmpty();
            if (free >= 0 && LoadoutMenu.inventorySlot(free) >= 0) chest.setItem(free, held);
        }
        Map<Integer, String> layout = new HashMap<>();
        for (int slot = 0; slot < LoadoutMenu.SIZE; slot++) {
            String key = keyOf(chest.getItem(slot));
            int at = LoadoutMenu.inventorySlot(slot);
            if (key != null && at >= 0) layout.put(at, key);
        }
        saveLayout(player, layout);
        player.sendActionBar(TextUtils.parse("<green>Loadout saved"));
    }

    private void saveLayout(Player player, Map<Integer, String> layout) {
        GauntletProfile profile = manager.profile(player);
        if (layout.equals(profile.layout)) return;
        profile.layout.clear();
        profile.layout.putAll(layout);
        UUID uuid = player.getUniqueId();
        db.async(() -> store.saveLayout(uuid, layout));
    }

    // ---- Using it ----

    // Gear and the menu, start and leave items are never dropped in the Gauntlet's world
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!manager.isHere(event.getPlayer())) return;
        String key = keyOf(event.getItemDrop().getItemStack());
        if (key != null && (key.startsWith(GEAR) || TOOL_SLOTS.containsKey(key))) event.setCancelled(true);
    }

    // The menu item opens the Gauntlet's menu, the start item starts a run at once, the
    // leave item leaves
    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String key = keyOf(event.getItem());
        if (key == null || !TOOL_SLOTS.containsKey(key)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        String problem = switch (key) {
            case START -> manager.runs() == null ? null : manager.runs().quickStart(player);
            case LEAVE -> leave(player);
            default -> {
                manager.openMenu(player);
                yield null;
            }
        };
        if (problem != null) TextUtils.error(player, problem);
    }

    // Out of the Gauntlet to the main spawn
    private String leave(Player player) {
        manager.leave(player);
        return null;
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

    // Walking into the PvP zone hands the setup out; leaving it (by /spawn's countdown, or
    // dying) puts it away again
    private void checkPvp(Player player, Location to) {
        if (!manager.isHere(player) || manager.inRun(player) || manager.inDuel(player)) {
            inPvp.remove(player.getUniqueId());
            return;
        }
        boolean now = manager.arena().inPvp(to);
        boolean was = now ? !inPvp.add(player.getUniqueId()) : inPvp.remove(player.getUniqueId());
        if (was == now) return;
        player.sendActionBar(TextUtils.parse(now ? "<red><b>PvP ZONE</b> <gray>your setup is out, leave the zone to put it away"
                : "<gray>Your setup is put away"));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !manager.isHere(player) || manager.inRun(player)) return;
            restore(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.6f, 1.2f);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        inPvp.remove(event.getPlayer().getUniqueId());
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
        TextUtils.send(player, setup.tool() == null
                ? "<green>You now start every run with <white>" + setup.count(next) + " " + setup.name()
                : "<green>Your " + setup.name() + " is now <white>level " + next);
        manager.refreshBoard(player);
        return null;
    }
}
