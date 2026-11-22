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
import org.charky.aurora.core.models.FishingSpotModel;
import org.charky.aurora.core.models.*;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.gauntlet.GauntletData;
import org.charky.aurora.features.keys.Keys;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.features.tool.Realms;
import org.charky.aurora.features.tool.ToolLevels;
import org.charky.aurora.features.tool.ToolType;
import org.charky.aurora.features.tool.UpgradeZone;
import org.charky.aurora.features.trader.TraderMenu;
import org.charky.aurora.features.warp.Warp;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

// All game data from the generated YAML, loaded through core.data and looked up here
public class DataRegistry {
    // One file per region under mobs/, generated from scripts/data/mobs/mobs_<name>.csv
    private static final List<String> MOB_FILES = List.of(
            "origin", "gormak", "verdant", "khorvath", "elderwood", "marsh",
            "peaks", "duat", "sky", "quarry", "gauntlet");

    private record BlockData(Material material, int density, int xp, List<LootPool> pools) {}

    private final Aurora plugin;
    private final YamlLoader yaml;
    private final ItemFactory itemFactory = new ItemFactory();
    private Function<String, MobData> mobVariants = id -> null;
    private GauntletData gauntlet = GauntletData.empty();
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
    // Fishing spots in the order they are played, see FishingManager
    private final Map<String, FishingSpotModel> fishingSpots = new LinkedHashMap<>();
    private final Map<String, ToolType> toolTypes = new LinkedHashMap<>();
    private Realms realms = new Realms(List.of());
    private final Map<String, UpgradeZone> upgradeZones = new LinkedHashMap<>();
    private Regions regions = Regions.none();
    // item id -> the region column it has in the CSVs, like gormak_city or origin_isles
    private final Map<String, String> itemRegions = new HashMap<>();

    // Resources in resources.csv order, which is progression order
    private final List<MenuItem> resourceList = new ArrayList<>();
    private final Set<String> resourceIds = new HashSet<>();
    private Keys keys = new Keys();
    // For a resource whose data has no collection amount yet
    private static final int DEFAULT_COLLECTION_BASE = 250;

    // resources_special.csv, keys are never boosted by Looting
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
        fishingSpots.clear();
        toolTypes.clear();
        upgradeZones.clear();
        itemRegions.clear();
        resourceList.clear();
        resourceIds.clear();
        keys = new Keys();
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
        fishingSpots.putAll(yaml.load("fishing.yml", FishingSpotModel.class));
        loadProgression();
        gauntlet = GauntletData.load(plugin, mobs.keySet());
    }

    // Realms, tools and every zone's upgrades, after the items their steps cost
    private void loadProgression() {
        ProgressionLoader.Progression progression = new ProgressionLoader(plugin.getLogger()).load(
                yaml.load("realms.yml", RealmModel.class), yaml.load("tools.yml", ToolModel.class),
                yaml.load("upgrades.yml", UpgradeModel.class), items, enchants::containsKey);
        realms = progression.realms();
        toolTypes.putAll(progression.tools());
        upgradeZones.putAll(progression.zones());
        regions = progression.regions();
    }

    // Which region each zone is in and which regions are released
    public Regions getRegions() {
        return regions;
    }

    // An item players are shown: one with no region, or from a released region. One
    // whose column names no region, like fishing, is switched off and hidden too
    public boolean isItemReleased(String itemId) {
        return regions.isColumnReleased(itemRegions.get(itemId.toLowerCase()));
    }

    // Every zone with upgrades, in zones.csv order
    public List<String> getUpgradeZoneIds() {
        return new ArrayList<>(upgradeZones.keySet());
    }

    // The tools a new player starts with: the ones the first zone upgrades
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
        return createTool(tool, stats, realm, null);
    }

    // Drawn in the look's material when the player has bought it for this tool, the
    // best one owned otherwise. It digs at the best one's speed whatever it shows
    public ItemStack createTool(ToolType tool, Map<String, Integer> stats, Realms.Realm realm, String look) {
        return createTool(tool, stats, realm == null ? "overworld" : realm.id(), look);
    }

    // As above, the lore's last line naming the world these levels are for: a realm, or
    // the Gauntlet for the tools its starting kit hands out
    public ItemStack createTool(ToolType tool, Map<String, Integer> stats, String world, String look) {
        int owned = stats.getOrDefault(ToolLevels.MATERIAL, 0);
        String best = ToolLevels.material(tool.materials(), owned);
        int lookLevel = look == null ? -1 : ToolLevels.materialLevel(tool.materials(), look);
        String material = lookLevel >= 0 && lookLevel <= owned ? tool.materials().get(lookLevel) : best;

        GearModel model = new GearModel();
        // A tool that only comes one way, a shield, has no material in its name
        boolean plain = material.equalsIgnoreCase(ToolLevels.NO_MATERIAL);
        model.display = "<white><b>" + (plain ? "" : material.charAt(0) + material.substring(1).toLowerCase() + " ") + tool.name();
        model.material = plain ? tool.item() : material + "_" + tool.item();
        if (Material.matchMaterial(model.material) == null) model.material = tool.materials().getFirst() + "_" + tool.item();
        model.type = tool.kind();
        // The last line of the lore, the world these levels are for
        model.world = world;
        model.enchants = new LinkedHashMap<>();
        stats.forEach((stat, level) -> {
            if (level > 0 && enchants.containsKey(stat)) model.enchants.put(stat, level);
        });

        ItemStack item = itemFactory.createGear(model, enchants);
        if (!material.equals(best)) {
            Material speed = Material.matchMaterial(best + "_" + tool.item());
            if (speed != null) ItemFactory.setSpeed(item, speed);
        }
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
    public Map<String, FishingSpotModel> getFishingSpots() {
        return fishingSpots;
    }

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
            keys.add(id, model.unlocks, model.summons);
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
        return getMob(id) != null;
    }

    // A mob from the files, or a variant made on the fly (a gauntlet mob on its floor,
    // see GauntletData.variant) for an id the files do not have
    public MobData getMob(String id) {
        MobData mob = mobs.get(id);
        return mob != null || id == null ? mob : mobVariants.apply(id);
    }

    public void setMobVariants(Function<String, MobData> variants) {
        this.mobVariants = variants;
    }

    // The Aurora mob an entity was spawned as, null for anything else
    public MobData mobOf(Entity entity) {
        String mobId = entity.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
        return mobId == null ? null : getMob(mobId);
    }

    // The gauntlet's floors, gear, boons, care packages and milestones
    public GauntletData getGauntlet() {
        return gauntlet;
    }

    // An Aurora gear item drawn from a model, with its enchants stored like any other
    public ItemStack createGear(GearModel model) {
        return itemFactory.createGear(model, enchants);
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

    // What the Essence Trader pays for one, 0 for what it does not buy
    public double getEssenceValue(String id) {
        return essenceValues.getOrDefault(id.toLowerCase(), 0.0);
    }

    // How many of a resource its first collection tier needs
    public int getCollectionBase(String id) {
        return collectionBases.getOrDefault(id.toLowerCase(), DEFAULT_COLLECTION_BASE);
    }

    // Looting widens resource drops, never keys, or a 1 in 12 key becomes 8 a kill
    public boolean isLootable(String id) {
        return isResource(id) && !specialIds.contains(id.toLowerCase());
    }

    // Passes and summoning keys, what right clicking them does
    public Keys getKeys() {
        return keys;
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
