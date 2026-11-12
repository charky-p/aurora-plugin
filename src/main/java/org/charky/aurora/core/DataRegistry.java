package org.charky.aurora.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.loot.LootPool;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.core.data.*;
import org.charky.aurora.core.models.*;
import org.charky.aurora.features.armory.ArmoryManager;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.trader.Trader;
import org.charky.aurora.features.trader.TraderMenu;

import java.util.*;

// All game data from the generated YAML, loaded through core.data and looked up here
public class DataRegistry {
    private record BlockData(Material material, int density, List<LootPool> pools) {}

    private final Aurora plugin;
    private final YamlLoader yaml;
    private final ItemFactory itemFactory = new ItemFactory();
    private BackpackManager backpackManager;
    private ArmoryManager armoryManager;
    private MenuActions actions;

    private final Map<String, ItemStack> items = new HashMap<>(); // resources + gear
    private final Map<String, EnchantData> enchants = new HashMap<>();
    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Map<String, MobData> mobs = new HashMap<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final Map<String, TraderMenu> guis = new HashMap<>();
    private final Map<String, Trader> traders = new HashMap<>();

    // Resources in resources.csv order, which is progression order
    private final List<MenuItem> resourceList = new ArrayList<>();
    private final Set<String> resourceIds = new HashSet<>();
    // Gear that mobs drop, in mob load order (progression), with the mob that drops it
    private final Map<String, Component> armory = new LinkedHashMap<>();

    public DataRegistry(Aurora plugin) {
        this.plugin = plugin;
        this.yaml = new YamlLoader(plugin);
    }

    public void setBackpackManager(BackpackManager backpackManager) {
        this.backpackManager = backpackManager;
    }

    public void setArmoryManager(ArmoryManager armoryManager) {
        this.armoryManager = armoryManager;
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
        resourceList.clear();
        resourceIds.clear();
        armory.clear();

        actions = new MenuActions(backpackManager, armoryManager);
        LootPoolBuilder loot = new LootPoolBuilder(items, this::isResource);

        loadResources();
        loadEnchants();
        loadGear("picks.yml");
        loadGear("armor.yml");
        loadGear("weapons.yml");
        loadGear("other.yml");
        loadBlocks(loot);

        MobLoader mobLoader = new MobLoader(plugin.getLogger(), items, loot, this::isResource);
        mobLoader.load(yaml.load("mobs/gormak.yml", MobModel.class), mobs, armory);
        mobLoader.load(yaml.load("mobs/origin.yml", MobModel.class), mobs, armory);
        mobLoader.load(yaml.load("mobs/verdant.yml", MobModel.class), mobs, armory);

        yaml.load("icons.yml", IconModel.class).forEach((id, model) -> icons.put(id, itemFactory.createIcon(model)));
        guis.putAll(new GuiLoader(plugin.getLogger(), items, icons, actions, backpackManager)
                .load(yaml.load("guis/shops.yml", GuiModel.class)));
        traders.putAll(new TraderLoader(plugin.getLogger()).load(yaml.load("traders.yml", TraderModel.class), guis));
    }

    private void loadResources() {
        yaml.load("resources.yml", ResourceModel.class).forEach((id, model) -> {
            ItemStack item = itemFactory.createResource(model);
            putItem(id, item);
            resourceList.add(new MenuItem(id.toLowerCase(), item));
            resourceIds.add(id.toLowerCase());
        });
    }

    private void loadEnchants() {
        yaml.load("enchants.yml", EnchantModel.class).forEach((id, model) -> enchants.put(id, new EnchantData(model.display,
                model.enchant == null ? null : Registry.ENCHANTMENT.get(NamespacedKey.minecraft(model.enchant.toLowerCase())))));
    }

    private void loadGear(String file) {
        yaml.load("gear/" + file, GearModel.class).forEach((id, model) -> putItem(id, itemFactory.createGear(model, enchants)));
    }

    // Blocks scale with Fortune instead, so nothing here is lootable
    private void loadBlocks(LootPoolBuilder loot) {
        yaml.load("blocks.yml", BlockModel.class).forEach((id, model) -> blocks.put(id.toLowerCase(), new BlockData(
                Material.matchMaterial(model.material), model.density, loot.build(model.independent, model.choices, false))));
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

    public List<MenuItem> getBlockDrops(String id, int fortune) {
        BlockData block = blocks.get(id);
        if (block == null) return new ArrayList<>();

        int multiplier = Math.max(0, fortune + 1 - block.density());
        return block.pools().stream()
                .map(LootPool::roll)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .peek(item -> item.setAmount(item.getAmount() * multiplier))
                .toList();
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

    public List<MenuItem> getMobDrops(String id, int looting) {
        return mobs.get(id).drops().roll(looting);
    }

    public Map<String, Component> getArmory() {
        return Collections.unmodifiableMap(armory);
    }

    // Resources stack in the backpack, so only they are boosted by Looting
    public boolean isResource(String id) {
        return resourceIds.contains(id.toLowerCase());
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
