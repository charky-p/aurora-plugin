package org.charky.aurora.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.loot.Fortune;
import org.charky.aurora.common.loot.LootPool;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.core.data.*;
import org.charky.aurora.core.models.*;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.features.tool.Realms;
import org.charky.aurora.features.tool.ToolLevels;
import org.charky.aurora.features.tool.ToolType;
import org.charky.aurora.features.tool.UpgradeZone;
import org.charky.aurora.features.warp.Warp;
import org.charky.aurora.core.models.RealmModel;
import org.charky.aurora.core.models.ToolModel;
import org.charky.aurora.core.models.UpgradeModel;
import org.charky.aurora.core.models.WarpModel;
import org.charky.aurora.features.trader.TraderMenu;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// All game data from the generated YAML, loaded through core.data and looked up here
public class DataRegistry {
    // One file per region under mobs/, generated from scripts/data/mobs/mobs_<name>.csv
    private static final List<String> MOB_FILES = List.of(
            "origin", "gormak", "verdant", "khorvath", "elderwood", "marsh",
            "peaks", "duat", "sky", "quarry");

    private record BlockData(Material material, int density, int xp, List<LootPool> pools) {}

    private final Aurora plugin;
    private final YamlLoader yaml;
    private final ItemFactory itemFactory = new ItemFactory();
    private BackpackManager backpackManager;
    private ArmoryManager armoryManager;
    private final MenuActions actions = new MenuActions();

    private final Map<String, ItemStack> items = new HashMap<>(); // resources + gear
    private final Map<String, EnchantData> enchants = new HashMap<>();
    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Map<String, MobData> mobs = new HashMap<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final Map<String, TraderMenu> guis = new HashMap<>();
    private final Map<String, Trader> traders = new HashMap<>();
    private final List<Warp> warps = new ArrayList<>();
    private final Map<String, ToolType> toolTypes = new LinkedHashMap<>();
    private Realms realms = new Realms(List.of());
    private final Map<String, UpgradeZone> upgradeZones = new LinkedHashMap<>();
    // zone id -> the region it has in zones.csv
    private final Map<String, String> zoneRegions = new LinkedHashMap<>();
    // Regions marked released in regions.csv, the only ones players are shown
    private final Set<String> releasedRegions = new HashSet<>();
    // item id -> the region column it has in the CSVs, like gormak_city or origin_isles
    private final Map<String, String> itemRegions = new HashMap<>();

    // Resources in resources.csv order, which is progression order
    private final List<MenuItem> resourceList = new ArrayList<>();
    private final Set<String> resourceIds = new HashSet<>();
    // Key items, item id to the tag right clicking it gives
    private final Map<String, String> unlocks = new LinkedHashMap<>();
    // Item id to the mob right clicking it summons
    private final Map<String, String> summons = new LinkedHashMap<>();
    // resources_special.csv, keys are never boosted by Looting
    // For a resource whose data has no collection amount yet
    private static final int DEFAULT_COLLECTION_BASE = 250;

    private final Set<String> specialIds = new HashSet<>();
    private final Map<String, Integer> collectionBases = new HashMap<>();
    private final Map<String, Double> essenceValues = new HashMap<>();
    // Gear that mobs drop, in mob load order (progression), with the mob that drops it
    private final Map<String, Component> armory = new LinkedHashMap<>();

    public DataRegistry(Aurora plugin) {
        this.plugin = plugin;
        this.yaml = new YamlLoader(plugin);
    }

    public void setBackpackManager(BackpackManager backpackManager) {
        this.backpackManager = backpackManager;
        registerAction("backpack", backpackManager::open);
    }

    public ArmoryManager getArmoryManager() {
        return armoryManager;
    }

    public void setArmoryManager(ArmoryManager armoryManager) {
        this.armoryManager = armoryManager;
    }

    // What an ACTION component of a menu does when clicked
    public void registerAction(String id, Consumer<Player> action) {
        actions.register(id, action);
    }

    // Order matters: gear needs enchants, drops need items, menus need icons and items
    public void loadAll() {
        items.clear();
        enchants.clear();
        blocks.clear();
        mobs.clear();
        icons.clear();
        guis.clear();
        traders.clear();
        warps.clear();
        toolTypes.clear();
        upgradeZones.clear();
        zoneRegions.clear();
        releasedRegions.clear();
        itemRegions.clear();
        resourceList.clear();
        resourceIds.clear();
        unlocks.clear();
        summons.clear();
        specialIds.clear();
        collectionBases.clear();
        essenceValues.clear();
        armory.clear();

        LootPoolBuilder loot = new LootPoolBuilder(plugin.getLogger(), items, this::isLootable, id -> !isResource(id));

        loadResources();
        loadEnchants();
        loadGear("armor.yml");
        loadGear("weapons.yml");
        loadGear("other.yml");
        loadBlocks(loot);

        MobLoader mobLoader = new MobLoader(plugin.getLogger(), items, loot, this::isResource);
        for (String file : MOB_FILES) mobLoader.load(yaml.load("mobs/" + file + ".yml", MobModel.class), mobs, armory);

        yaml.load("icons.yml", IconModel.class).forEach((id, model) -> icons.put(id, itemFactory.createIcon(model)));
        guis.putAll(new GuiLoader(plugin.getLogger(), items, icons, actions, backpackManager)
                .load(yaml.load("guis/shops.yml", GuiModel.class)));
        traders.putAll(new TraderLoader(plugin.getLogger()).load(yaml.load("traders.yml", TraderModel.class), guis, actions));
        loadWarps();
        loadTools();
        loadUpgrades();
    }

    private void loadTools() {
        List<Realms.Realm> loaded = new ArrayList<>();
        yaml.load("realms.yml", RealmModel.class).forEach((id, model) ->
                loaded.add(new Realms.Realm(id.toLowerCase(), model.name, List.copyOf(model.worlds), model.base, model.part_of)));
        realms = new Realms(loaded);

        yaml.load("tools.yml", ToolModel.class).forEach((id, model) -> {
            if (model.materials.isEmpty() || model.item == null) {
                plugin.getLogger().severe("TOOL ERROR: '" + id + "' needs an item and at least one material");
                return;
            }
            toolTypes.put(id.toLowerCase(), new ToolType(id.toLowerCase(), model.name, model.kind,
                    model.item.toUpperCase(), List.copyOf(model.materials)));
        });
    }

    // After the tools, a step for a tool or material that does not exist is skipped
    private void loadUpgrades() {
        yaml.load("upgrades.yml", UpgradeModel.class).forEach((id, model) -> {
            List<UpgradeZone.Step> steps = new ArrayList<>();
            for (UpgradeModel.StepModel step : model.steps) {
                ToolType tool = getToolType(step.tool);
                int level = tool == null ? -1 : upgradeLevel(tool, step.stat, step.level);
                if (level < 0) {
                    plugin.getLogger().severe("UPGRADE ERROR: '" + id + "' " + step.tool + " " + step.stat + " " + step.level);
                    continue;
                }

                List<MenuItem> costs = new ArrayList<>();
                for (GuiModel.CostEntry entry : step.cost) {
                    ItemStack item = items.get(entry.id.toLowerCase());
                    if (item == null) {
                        plugin.getLogger().severe("UPGRADE ERROR: '" + id + "' costs unknown item '" + entry.id + "'");
                        continue;
                    }
                    MenuItem cost = new MenuItem(entry.id.toLowerCase(), item);
                    cost.setAmount(entry.amount);
                    costs.add(cost);
                }
                steps.add(new UpgradeZone.Step(tool.id(), step.stat.toLowerCase(), level, costs));
            }
            List<String> before = model.before == null ? List.of() : model.before.stream().map(String::toLowerCase).toList();
            upgradeZones.put(id.toLowerCase(), new UpgradeZone(id.toLowerCase(), model.name,
                    model.realm == null ? "" : model.realm.toLowerCase(), before, steps));
            String region = model.region == null ? "" : model.region.toLowerCase();
            zoneRegions.put(id.toLowerCase(), region);
            if (model.released) releasedRegions.add(region);
        });
    }

    // The world a zone is part of, as zones.csv names it
    public String getZoneRegion(String zoneId) {
        return zoneRegions.getOrDefault(zoneId, "");
    }

    // ----------------------------------------------------------------- releases

    // A region marked released in regions.csv. Players are only shown these
    public boolean isRegionReleased(String region) {
        return region != null && releasedRegions.contains(region.toLowerCase());
    }

    public boolean isZoneReleased(String zoneId) {
        return isRegionReleased(getZoneRegion(zoneId));
    }

    // The released zones in zones.csv order
    public List<String> getReleasedZoneIds() {
        return upgradeZones.keySet().stream().filter(this::isZoneReleased).toList();
    }

    // The region a CSV region column means: the region itself (gormak) or one of its
    // zones (gormak_city). Null for one that is no region, like fishing
    public String regionOf(String column) {
        if (column == null || column.isBlank()) return null;
        String best = null;
        for (String region : zoneRegions.values()) {
            if ((column.equals(region) || column.startsWith(region + "_")) && (best == null || region.length() > best.length())) {
                best = region;
            }
        }
        return best;
    }

    // An item players are shown: one with no region, or from a released region. One
    // whose column names no region, like fishing, is switched off and hidden too
    public boolean isItemReleased(String itemId) {
        String column = itemRegions.get(itemId.toLowerCase());
        if (column == null || column.isBlank()) return true;
        return isRegionReleased(regionOf(column));
    }

    public List<String> getZonesOfRegion(String region) {
        return zoneRegions.entrySet().stream().filter(entry -> entry.getValue().equals(region))
                .map(Map.Entry::getKey).toList();
    }

    // -1 for a material the tool does not come in, an unknown enchant or a bad number
    private int upgradeLevel(ToolType tool, String stat, String level) {
        if (stat == null || level == null) return -1;
        if (stat.equalsIgnoreCase(ToolLevels.MATERIAL)) return ToolLevels.materialLevel(tool.materials(), level);
        if (stat.equalsIgnoreCase(ToolLevels.UNLOCK)) return 1;
        if (!enchants.containsKey(stat.toLowerCase())) return -1;
        try {
            return Integer.parseInt(level);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // The tools a new player starts with: the ones the first zone upgrades
    public List<String> getUpgradeZoneIds() {
        return new ArrayList<>(upgradeZones.keySet());
    }

    public List<ToolType> getStarterTools() {
        List<ToolType> starters = new ArrayList<>();
        if (upgradeZones.isEmpty()) return starters;
        for (String id : upgradeZones.values().iterator().next().tools()) {
            ToolType tool = getToolType(id);
            if (tool != null) starters.add(tool);
        }
        return starters;
    }

    // Null for a zone with no upgrades
    public UpgradeZone getUpgradeZone(String id) {
        return id == null ? null : upgradeZones.get(id.toLowerCase());
    }

    // "Fortune" as the enchant file shows it, the id for one that is not there
    public String getEnchantDisplay(String id) {
        EnchantData enchant = enchants.get(id.toLowerCase());
        return enchant == null ? id : enchant.display();
    }

    public void registerAction(String name, BiConsumer<Player, String> action) {
        actions.register(name, action);
    }

    public Realms getRealms() {
        return realms;
    }

    // Null for an id that is not a tool
    public ToolType getToolType(String id) {
        return id == null ? null : toolTypes.get(id.toLowerCase());
    }

    public List<String> getToolIds() {
        return new ArrayList<>(toolTypes.keySet());
    }

    public boolean hasEnchant(String id) {
        return enchants.containsKey(id.toLowerCase());
    }

    public List<String> getEnchantIds() {
        return new ArrayList<>(enchants.keySet());
    }

    // The item a tool is drawn as: the material its level stands for, and every
    // enchant level written on it the way gear from the files has them, so mining
    // and combat read it like any other item. The realm may be null
    public ItemStack createTool(ToolType tool, Map<String, Integer> stats, Realms.Realm realm) {
        String material = ToolLevels.material(tool.materials(), stats.getOrDefault(ToolLevels.MATERIAL, 0));

        GearModel model = new GearModel();
        // A tool that only comes one way, a shield, has no material in its name
        boolean plain = material.equalsIgnoreCase(ToolLevels.NO_MATERIAL);
        model.display = "<white><b>" + (plain ? "" : material.charAt(0) + material.substring(1).toLowerCase() + " ") + tool.name();
        model.material = plain ? tool.item() : material + "_" + tool.item();
        if (Material.matchMaterial(model.material) == null) model.material = tool.materials().getFirst() + "_" + tool.item();
        model.type = tool.kind();
        // The last line of the lore, the realm these levels are for
        model.world = realm == null ? "overworld" : realm.id();
        model.enchants = new LinkedHashMap<>();
        stats.forEach((stat, level) -> {
            if (level > 0 && enchants.containsKey(stat)) model.enchants.put(stat, level);
        });

        ItemStack item = itemFactory.createGear(model, enchants);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(DataKeys.TOOL_KEY, PersistentDataType.STRING, tool.id());
            meta.getPersistentDataContainer().set(DataKeys.ITEM_KEY, PersistentDataType.STRING, "tool_" + tool.id());
        });
        return item;
    }

    private void loadWarps() {
        yaml.load("warps.yml", WarpModel.class).forEach((id, model) -> {
            ItemStack icon = model.icon == null ? null : icons.get(model.icon.toLowerCase());
            if (icon == null || model.world == null) {
                plugin.getLogger().severe("WARP ERROR: '" + id + "' needs a world and a known icon");
                return;
            }
            String tag = model.tag == null || model.tag.isBlank() ? null : model.tag.toLowerCase();
            String region = model.region == null ? "" : model.region.toLowerCase();
            warps.add(new Warp(id.toLowerCase(), icon, model.world, region, tag));
        });
    }

    // In warps.csv order, which is the order players reach the regions
    public List<Warp> getWarps() {
        return List.copyOf(warps);
    }

    // Special resources go after the rest, they are the same items but do something
    // when right clicked
    private void loadResources() {
        Map<String, ResourceModel> models = new LinkedHashMap<>(yaml.load("resources.yml", ResourceModel.class));
        Map<String, ResourceModel> special = yaml.load("resources_special.yml", ResourceModel.class);
        special.keySet().forEach(id -> specialIds.add(id.toLowerCase()));
        models.putAll(special);
        models.forEach((id, model) -> {
            ItemStack item = itemFactory.createResource(model);
            putItem(id, item);
            resourceList.add(new MenuItem(id.toLowerCase(), item));
            resourceIds.add(id.toLowerCase());
            if (model.region != null) itemRegions.put(id.toLowerCase(), model.region.toLowerCase());
            if (model.collection > 0) collectionBases.put(id.toLowerCase(), model.collection);
            if (model.essence > 0) essenceValues.put(id.toLowerCase(), model.essence);
            if (model.unlocks != null && !model.unlocks.isBlank()) unlocks.put(id.toLowerCase(), model.unlocks.toLowerCase());
            if (model.summons != null && !model.summons.isBlank()) summons.put(id.toLowerCase(), model.summons.toLowerCase());
        });
    }

    private void loadEnchants() {
        yaml.load("enchants.yml", EnchantModel.class).forEach((id, model) -> enchants.put(id, new EnchantData(model.display,
                model.enchant == null ? null : Registry.ENCHANTMENT.get(NamespacedKey.minecraft(model.enchant.toLowerCase())))));
    }

    private void loadGear(String file) {
        yaml.load("gear/" + file, GearModel.class).forEach((id, model) -> {
            putItem(id, itemFactory.createGear(model, enchants));
            if (model.region != null) itemRegions.put(id.toLowerCase(), model.region.toLowerCase());
        });
    }

    // Blocks scale with Fortune instead, so nothing here is lootable
    private void loadBlocks(LootPoolBuilder loot) {
        // The xp column of blocks.csv is what each item the block drops is worth. The
        // blocks of one zone share a number, so a log levels as fast as its zone's metal
        yaml.load("blocks.yml", BlockModel.class).forEach((id, model) ->
                blocks.put(id.toLowerCase(), new BlockData(Material.matchMaterial(model.material), model.density,
                        Math.max(1, model.xp), loot.build(id, model.independent, model.choices, false))));
    }


    // Every registry item carries its id, so costs match what an item is rather than
    // every detail of how it looks, which changes whenever gear generation does
    private void putItem(String id, ItemStack item) {
        String key = id.toLowerCase();
        item.editMeta(meta -> meta.getPersistentDataContainer().set(DataKeys.ITEM_KEY, PersistentDataType.STRING, key));
        items.put(key, item);
    }

    // The registry id of an item, null for anything that did not come from the registry
    public static String itemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(DataKeys.ITEM_KEY, PersistentDataType.STRING);
    }

    public List<String> getBlocks(String prefix) {
        return blocks.keySet().stream()
                .filter(key -> key.toLowerCase().startsWith(prefix))
                .sorted()
                .toList();
    }

    public boolean hasBlock(String id) {
        return blocks.containsKey(id);
    }

    public Material getMaterial(String id) {
        return blocks.get(id).material();
    }

    public int getDensity(String id) {
        BlockData block = blocks.get(id);
        return block == null ? 0 : block.density();
    }

    // XP for mining it, the number of the mine it belongs to
    public int getBlockXp(String id) {
        BlockData block = blocks.get(id);
        return block == null ? 0 : block.xp();
    }

    public List<MenuItem> getBlockDrops(String id, int fortune) {
        BlockData block = blocks.get(id);
        if (block == null) return new ArrayList<>();

        double multiplier = Fortune.multiplier(fortune, block.density());
        if (multiplier == 0) return new ArrayList<>();
        double rareMultiplier = Fortune.rareMultiplier(fortune, block.density());

        List<MenuItem> drops = new ArrayList<>();
        for (LootPool pool : block.pools()) {
            for (MenuItem item : pool.roll()) {
                item.setAmount(pool.isRare()
                        ? Fortune.scaleRare(item.getAmount(), rareMultiplier, ThreadLocalRandom.current())
                        : Fortune.scale(item.getAmount(), multiplier));
                drops.add(item);
            }
        }
        return drops;
    }

    public boolean hasMob(String id) {
        return mobs.containsKey(id);
    }

    public MobData getMob(String id) {
        return mobs.get(id);
    }

    // The Aurora mob an entity was spawned as, null for anything else
    public MobData mobOf(Entity entity) {
        String mobId = entity.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
        return mobId == null ? null : mobs.get(mobId);
    }

    public List<String> getMobs() {
        return new ArrayList<>(mobs.keySet());
    }

    public List<MenuItem> getMobDrops(String id, int looting, int luck) {
        return mobs.get(id).drops().roll(looting, luck);
    }

    public Map<String, Component> getArmory() {
        return Collections.unmodifiableMap(armory);
    }

    // Resources stack in the backpack
    public boolean isResource(String id) {
        return resourceIds.contains(id.toLowerCase());
    }

    // Looting widens resource drops, never keys, or a 1 in 12 key becomes 8 a kill
    // How many of a resource its first collection tier needs
    // What the Essence Trader pays for one, 0 for what it does not buy
    public double getEssenceValue(String id) {
        return essenceValues.getOrDefault(id.toLowerCase(), 0.0);
    }

    public int getCollectionBase(String id) {
        return collectionBases.getOrDefault(id.toLowerCase(), DEFAULT_COLLECTION_BASE);
    }

    public boolean isLootable(String id) {
        return isResource(id) && !specialIds.contains(id.toLowerCase());
    }

    // The tag a key item gives, null if it is not a key
    // Every tag a key gives, the ones locked portals ask for
    public List<String> getUnlockTags() {
        return unlocks.values().stream().distinct().toList();
    }

    public String getUnlock(String itemId) {
        return itemId == null ? null : unlocks.get(itemId.toLowerCase());
    }

    // The mob a key item summons, null if it summons nothing
    public String getSummon(String itemId) {
        return itemId == null ? null : summons.get(itemId.toLowerCase());
    }

    // Summoned mobs only come from their key, their spawners never spawn them
    public boolean isSummoned(String mobId) {
        return mobId != null && summons.containsValue(mobId.toLowerCase());
    }

    // Any key, something that does a thing when right clicked
    // Every key in data order
    public List<String> getKeyIds() {
        return resourceList.stream().map(MenuItem::getId).filter(this::isKey).toList();
    }

    public boolean isKey(String itemId) {
        return getUnlock(itemId) != null || getSummon(itemId) != null;
    }

    // The items that give a tag, for telling players what they are missing
    public List<String> getKeysFor(String tag) {
        return unlocks.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(tag))
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<MenuItem> getItems() {
        return resourceList;
    }

    public Map<String, ItemStack> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    public MenuItem getItem(String item) {
        return new MenuItem(item, items.get(item));
    }

    public Map<String, ItemStack> getAllIcons() {
        return Collections.unmodifiableMap(icons);
    }

    public TraderMenu getGui(String id) {
        return guis.get(id);
    }

    public void runAction(String id, Player player) {
        actions.run(id, player);
    }

    public boolean hasTrader(String id) {
        return traders.containsKey(id);
    }

    public Trader getTrader(String id) {
        return traders.get(id);
    }

    public List<String> getTraders() {
        return new ArrayList<>(traders.keySet());
    }
}
