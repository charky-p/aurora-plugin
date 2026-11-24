package org.charky.aurora.features.fishing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.Swaps;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.FishingStore;
import org.charky.aurora.core.models.FishingSpotModel;
import org.charky.aurora.core.models.FishingSpotModel.CostModel;
import org.charky.aurora.core.models.FishingSpotModel.FishModel;
import org.charky.aurora.core.models.FishingSpotModel.RodStepModel;
import org.charky.aurora.features.collection.CollectionManager;
import org.charky.aurora.features.loadout.LoadoutData;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.BackpackManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

// Fishing, a side quest apart from the zones. One spot a realm (fishing.yml), each
// with its own water marked with /aurora fishspot, its own fish and its own rod
// upgrades. The rod is one item whose levels live on the player like a tool's, so
// it gets better spot after spot:
//  - Fortune: how many come up, past each fish's density, like a pickaxe's
//  - Luck: added to the roll that picks the fish, so rarer ones bite
//  - Lure: shortens the wait for a bite
// Fish go to the backpack and collections; the Fish Merchant buys them for Essence
public class FishingManager implements Listener, LoadoutData {
    public static final String ROD_ID = "fishing_rod";
    public static final List<String> STATS = List.of("fortune", "luck", "lure");
    private static final NamespacedKey ENCHANT_PREFIX = DataKeys.ENCHANT_KEY;

    private final DataRegistry registry;
    private final Database db;
    private final FishingStore store;
    private final BackpackManager backpacks;
    private final CollectionManager collections;
    private final Map<UUID, Map<String, Integer>> rods;
    private final Map<String, FishingArea> areas;

    public FishingManager(DataRegistry registry, Database db, FishingStore store, BackpackManager backpacks,
                          CollectionManager collections) {
        this.registry = registry;
        this.db = db;
        this.store = store;
        this.backpacks = backpacks;
        this.collections = collections;
        this.rods = new ConcurrentHashMap<>(store.loadRods());
        this.areas = new ConcurrentHashMap<>(store.loadAreas());
    }

    public int level(UUID uuid, String stat) {
        return rods.getOrDefault(uuid, Map.of()).getOrDefault(stat, 0);
    }

    public Map<String, FishingSpotModel> spots() {
        return registry.getFishingSpots();
    }

    // ---------------------------------------------------------------- the rod

    public ItemStack rod(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic><aqua><b>Fishing Rod"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtils.parse("<!italic><dark_gray>➶ Fishing"));
            lore.add(Component.empty());
            for (String stat : STATS) {
                lore.add(TextUtils.parse("<!italic><gray>»<white> " + title(stat) + " " + level(uuid, stat)));
            }
            lore.add(Component.empty());
            lore.add(TextUtils.parse("<!italic><gray>Cast at a fishing spot's water."));
            lore.add(TextUtils.parse("<!italic><gray>Its trader sells the next upgrades."));
            meta.lore(lore);
            meta.setUnbreakable(true);
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(DataKeys.ITEM_KEY, PersistentDataType.STRING, ROD_ID);
            PersistentDataContainer enchants = pdc.getAdapterContext().newPersistentDataContainer();
            for (String stat : STATS) {
                enchants.set(new NamespacedKey("aurora", stat), PersistentDataType.INTEGER, level(uuid, stat));
            }
            pdc.set(ENCHANT_PREFIX, PersistentDataType.TAG_CONTAINER, enchants);
        });
        return item;
    }

    public static boolean isRod(ItemStack item) {
        return ROD_ID.equals(DataRegistry.itemId(item));
    }

    public boolean carriesRod(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isRod(item)) return true;
        }
        return false;
    }

    // False if the inventory is full
    public boolean giveRod(Player player) {
        return player.getInventory().addItem(rod(player)).isEmpty();
    }

    // Every rod carried redrawn with the levels it has now
    public void refreshRods(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isRod(inventory.getItem(slot))) inventory.setItem(slot, rod(player));
        }
    }

    // ---------------------------------------------------------------- spots

    public FishingArea areaAt(Location at) {
        for (FishingArea area : areas.values()) {
            if (area.contains(at)) return area;
        }
        return null;
    }

    public Map<String, FishingArea> areas() {
        return areas;
    }

    // An error to show, null when it was made
    public String createArea(String name, String spot, Location a, Location b) {
        if (!spots().containsKey(spot)) return "No fishing spot '" + spot + "', see fishing.yml: " + String.join(", ", spots().keySet());
        if (areas.containsKey(name)) return "A fishing area called '" + name + "' already exists";
        if (a == null || b == null || a.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return "Select both corners with the wand first, in one world";
        }
        FishingArea area = FishingArea.of(name, spot, a, b);
        areas.put(name, area);
        db.async(() -> store.saveArea(area));
        return null;
    }

    public boolean deleteArea(String name) {
        if (areas.remove(name) == null) return false;
        db.async(() -> store.deleteArea(name));
        return true;
    }

    // ---------------------------------------------------------------- casting

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!isRod(player.getInventory().getItemInMainHand()) && !isRod(player.getInventory().getItemInOffHand())) return;

        FishHook hook = event.getHook();
        UUID uuid = player.getUniqueId();
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            int[] wait = FishingRules.biteRange(level(uuid, "lure"));
            hook.setWaitTime(wait[0], wait[1]);
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        // Never vanilla's fish, books or XP
        if (event.getCaught() != null) event.getCaught().remove();
        event.setExpToDrop(0);

        FishingArea area = areaAt(hook.getLocation());
        FishingSpotModel spot = area == null ? null : spots().get(area.spot());
        if (spot == null) {
            player.sendActionBar(TextUtils.parse("<red>Nothing worth keeping bites here, fish at a fishing spot"));
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        FishModel fish = FishingRules.bite(spot.fish, random.nextInt(1, 101), level(uuid, "luck"));
        if (fish == null) return;
        int amount = FishingRules.amount(fish, random.nextInt(fish.min, fish.max + 1), level(uuid, "fortune"));
        ItemStack item = registry.getAllItems().get(fish.id);
        if (item == null) return;
        if (amount <= 0) {
            player.sendActionBar(TextUtils.parse("<red>It got away! ").append(item.effectiveName())
                    .append(TextUtils.parse(" <red>needs Fortune " + fish.density)));
            return;
        }

        List<MenuItem> drops = List.of(new MenuItem(fish.id, item, amount));
        backpacks.get(uuid).collect(player, drops);
        collections.add(player, drops);
        if ("rare".equals(fish.tier)) {
            player.sendMessage(TextUtils.parse("<gold><b>RARE CATCH!</b> <gray>You reeled in ").append(item.effectiveName()));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        }
    }

    // ---------------------------------------------------------------- the shop

    // The next upgrade this spot sells, null when its rod is maxed here
    public RodStepModel next(UUID uuid, FishingSpotModel spot) {
        for (RodStepModel step : spot.rod) {
            if (level(uuid, step.stat) < step.level) return step;
        }
        return null;
    }

    // The spot played before this one whose upgrades are not all bought, null if none
    public FishingSpotModel unfinishedBefore(UUID uuid, FishingSpotModel spot) {
        for (FishingSpotModel other : spots().values()) {
            if (other.order < spot.order && next(uuid, other) != null) return other;
        }
        return null;
    }

    public List<MenuItem> costs(RodStepModel step) {
        List<MenuItem> costs = new ArrayList<>();
        for (CostModel cost : step.costs) {
            ItemStack item = registry.getAllItems().get(cost.id);
            if (item != null) costs.add(new MenuItem(cost.id, item, cost.amount));
        }
        return costs;
    }

    // Buys the spot's next upgrade. An error to show, null when bought
    public String buy(Player player, String spotId) {
        FishingSpotModel spot = spots().get(spotId);
        if (spot == null) return "There is no fishing here yet.";
        UUID uuid = player.getUniqueId();
        FishingSpotModel before = unfinishedBefore(uuid, spot);
        if (before != null) return "Finish the rod at " + plain(before.name) + " first";
        RodStepModel step = next(uuid, spot);
        if (step == null) return "Your rod is maxed here, the next spot sells more.";
        BackpackData backpack = backpacks.get(uuid);
        List<MenuItem> costs = costs(step);
        if (backpack == null || !backpack.hasItems(player, costs)) return "Not enough fish yet";

        backpack.removeItems(player, costs);
        rods.computeIfAbsent(uuid, k -> new HashMap<>()).put(step.stat, step.level);
        db.async(() -> store.saveLevel(uuid, step.stat, step.level));
        refreshRods(player);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);
        return null;
    }

    // A spot's name without its colours
    public static String plain(String name) {
        return PlainTextComponentSerializer.plainText().serialize(TextUtils.parse(name));
    }

    public static String title(String stat) {
        return Character.toUpperCase(stat.charAt(0)) + stat.substring(1);
    }

    // ---------------------------------------------------------------- loadouts

    @Override
    public void swapLoadout(Player player, UUID other) {
        Swaps.keys(rods, player.getUniqueId(), other);
    }
}
