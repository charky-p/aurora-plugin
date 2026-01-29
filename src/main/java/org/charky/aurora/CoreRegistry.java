package org.charky.aurora;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.features.loot.CumulativeLootPool;
import org.charky.aurora.features.loot.IndependentLootPool;
import org.charky.aurora.features.loot.ItemDrop;
import org.charky.aurora.features.loot.MenuItem;
import org.charky.aurora.features.mob.MobData;
import org.charky.aurora.features.trader.*;
import org.charky.aurora.interfaces.LootPool;
import org.charky.aurora.interfaces.TraderNode;
import org.charky.aurora.models.BlockModel;
import org.charky.aurora.models.GuiModel;
import org.charky.aurora.models.MobModel;
import org.charky.aurora.util.ItemFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoreRegistry {
    private final Aurora plugin;
    private final ItemFactory itemFactory;
    private record BlockData(Material material, List<LootPool> pools) {}
    private final Map<String, ItemStack> items = new HashMap<>(); // resources + gear
    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Map<String, MobData> mobs = new HashMap<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final Map<String, TraderMenu> guis = new HashMap<>();
    private final Map<String, Trader> traders = new HashMap<>();

    public CoreRegistry(Aurora plugin) {
        this.plugin = plugin;
        this.itemFactory = plugin.getItemFactory();
    }

    public void loadAll() {
        items.clear();
        blocks.clear();
        mobs.clear();
        icons.clear();
        guis.clear();
        traders.clear();

        loadResources();
        loadGear();
        loadBlocks();
        loadMobs();
        loadIcons();
        loadNodes();
        loadTraders();

        debugRegistry();
    }

    public void debugRegistry() {
        var logger = plugin.getLogger();
        logger.info("=== [ Aurora Registry Debug Report ] ===");

        // 1. Items (Resources + Gear)
        printMapInfo("Items", items);

        // 2. Blocks
        printMapInfo("Blocks", blocks);

        // 3. Mobs
        printMapInfo("Mobs", mobs);

        // 4. GUI Icons
        printMapInfo("Icons", icons);

        // 5. Trader Nodes (GUIs)
        printMapInfo("GUIs/Menus", guis);

        // 6. Traders (Physical Entities)
        logger.info("-> Traders [" + traders.size() + "]:");
        traders.forEach((id, trader) -> {
            logger.info("   - " + id + " (Links to Menu: " + trader.getMenuId() + ")");
        });

        logger.info("=== [ Debug Report Complete ] ===");
    }

    private void printMapInfo(String label, Map<String, ?> map) {
        var logger = plugin.getLogger();
        logger.info("-> " + label + " [" + map.size() + "]:");
        if (map.isEmpty()) {
            logger.warning("   !! NO " + label.toUpperCase() + " LOADED !!");
        } else {
            // Prints the first 20 keys to avoid spamming console if you have thousands
            List<String> keys = new ArrayList<>(map.keySet());
            for (int i = 0; i < Math.min(keys.size(), 20); i++) {
                logger.info("   - " + keys.get(i));
            }
            if (keys.size() > 20) {
                logger.info("   ... and " + (keys.size() - 20) + " more.");
            }
        }
    }

    public void loadResources() {
        ResourceFile data = loadFile("resources.yml", ResourceFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            ItemStack item = itemFactory.createItem(
                    model.display,
                    Material.matchMaterial(model.material),
                    model.lore,
                    model.world
            );

            items.put("aurora:" + id.toLowerCase(), item);
        });
    }

    public void loadGear() {
        GearFile data = loadFile("gear.yml", GearFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            List<String> loreStrings = model.lore.stream().map(l -> l.display).toList();

            ItemStack item = itemFactory.createGear(
                    model.display,
                    Material.matchMaterial(model.material),
                    model.tier,
                    model.type,
                    model.world,
                    loreStrings,
                    model.enchants
            );

            items.put("aurora:" + id.toLowerCase(), item);
        });
    }

    public void loadBlocks() {
        BlockFile data = loadFile("blocks.yml", BlockFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            Material material = Material.matchMaterial(model.material);

            List<LootPool> pools = new ArrayList<>();
            if (!model.independent.isEmpty()) {
                IndependentLootPool pool = new IndependentLootPool();
                for (BlockModel.IndependentPoolModel choice : model.independent) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(items.get(choice.id), choice.min, choice.max), choice.chance);
                }
                pools.add(pool);
            }

            if (model.choices != null) {
                CumulativeLootPool pool = new CumulativeLootPool(model.choices.chance);
                for (BlockModel.ChoiceModel choice : model.choices.options) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(items.get(choice.id), choice.min, choice.max), choice.weight);
                }
                pools.add(pool);
            }

            blocks.put("aurora:" + id.toLowerCase(), new BlockData(material, pools));
        });
    }

    public void loadMobs() {
        MobFile data = loadFile("mobs.yml", MobFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            EntityType type = EntityType.fromName(model.entity);
            Component displayName = MiniMessage.miniMessage().deserialize(model.display);
            Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
            if (model.equip != null) {
                if (model.equip.head != null && items.containsKey(model.equip.head)) {
                    equipment.put(EquipmentSlot.HEAD, items.get(model.equip.head));
                }

                if (model.equip.chest != null && items.containsKey(model.equip.chest)) {
                    equipment.put(EquipmentSlot.CHEST, items.get(model.equip.chest));
                }

                if (model.equip.legs != null && items.containsKey(model.equip.legs)) {
                    equipment.put(EquipmentSlot.LEGS, items.get(model.equip.legs));
                }

                if (model.equip.feet != null && items.containsKey(model.equip.feet)) {
                    equipment.put(EquipmentSlot.FEET, items.get(model.equip.feet));
                }

                if (model.equip.hand != null && items.containsKey(model.equip.hand)) {
                    equipment.put(EquipmentSlot.HAND, items.get(model.equip.hand));
                }

                if (model.equip.off_hand != null && items.containsKey(model.equip.off_hand)) {
                    equipment.put(EquipmentSlot.OFF_HAND, items.get(model.equip.off_hand));
                }
            }

            Map<Attribute, Double> attributes = new HashMap<>();
            if (model.attributes != null) {
                for (MobModel.AttributeEntry entry : model.attributes) {
                    NamespacedKey key = NamespacedKey.fromString(entry.attr);
                    if (key == null) continue;
                    attributes.put(Registry.ATTRIBUTE.get(key), entry.value);
                }
            }

            List<LootPool> pools = new ArrayList<>();
            if (!model.independent.isEmpty()) {
                IndependentLootPool pool = new IndependentLootPool();
                for (BlockModel.IndependentPoolModel choice : model.independent) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(items.get(choice.id), choice.min, choice.max), choice.chance);
                }
                pools.add(pool);
            }

            if (model.choices != null) {
                CumulativeLootPool pool = new CumulativeLootPool(model.choices.chance);
                for (BlockModel.ChoiceModel choice : model.choices.options) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(items.get(choice.id), choice.min, choice.max), choice.weight);
                }
                pools.add(pool);
            }

            // TODO: Not implemented with multiple pools, only one
            MobData mobData = new MobData(id, type, displayName, equipment, attributes, pools.getFirst());
        });
    }

    public void loadIcons() {
        IconFile data = loadFile("icons.yml", IconFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            Component display = MiniMessage.miniMessage().deserialize(model.display);
            Material material = Material.matchMaterial(model.material);
            ItemStack item = new ItemStack(material);
            item.editMeta(meta -> {
               meta.displayName(display);
               List<Component> lore = new ArrayList<>();
               for (String line : model.lore) {
                   lore.add(MiniMessage.miniMessage().deserialize(line));
               }

               if (model.glow) meta.addEnchant(Enchantment.SHARPNESS, 1, true);
               meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
               meta.lore(lore);
            });

            icons.put(id, item);
        });
    }

    public void loadNodes() {
        GuiFile data = loadFile("guis.yml", GuiFile.class);
        if (data == null || data.items == null) return;

        // Make map for look up existence
        data.items.forEach((id, model) -> {
            Component title = MiniMessage.miniMessage().deserialize(model.title);
            ItemStack icon = icons.get(model.icon);
            guis.put(id, new TraderMenu(id, title, model.rows, new HashMap<>(), icon));
        });

        data.items.forEach((id, model) -> {
            if (!icons.containsKey(model.icon)) return;
            Component displayTitle = MiniMessage.miniMessage().deserialize(model.title);

            Map<Integer, TraderNode> contents = new HashMap<>();
            for (int row = 0; row < model.layout.size(); row++) {
                String rowStr = model.layout.get(row);
                for (int col = 0; col < rowStr.length(); col++) {
                    // DO CHECK ON 'B', 'I', 'X', then map distincts
                    if (rowStr.charAt(col) == 'B') {
                        contents.put(row * 9 + col, new TraderBorder());
                    } else if (rowStr.charAt(col) == 'I') {
                        contents.put(row * 9 + col, new TraderInfo());
                    } else if (rowStr.charAt(col) == 'X') {
                        contents.put(row * 9 + col, new TraderClose());
                    } else if (rowStr.charAt(col) != ' '){
                        GuiModel.ComponentModel component = model.components.get(rowStr.charAt(col));
                        if (component.type.equals("GUI")) {
                            // should never be called unless error with yaml
                            if (!guis.containsKey(component.id)) {
                                contents.put(row * 9 + col, new TraderBorder());
                                continue;
                            }
                            contents.put(row * 9 + col, guis.get(component.id));
                        } else {
                            // Purchasable item
                            // Error happened with yaml, TODO: better error handling with yaml
                            if (!items.containsKey(component.id)) {
                                contents.put(row * 9 + col, new TraderBorder());
                                continue;
                            }
                            MenuItem menu = new MenuItem(items.get(component.id), component.amount);
                            List<MenuItem> costs = new ArrayList<>();
                            for (GuiModel.CostEntry entry : component.cost) {
                                if (!items.containsKey(component.id)) continue;
                                costs.add(new MenuItem(items.get(entry.id), entry.amount));
                            }
                            contents.put(row * 9 + col, new TraderItem(component.id, menu, costs));
                        }
                    }
                }
            }

            TraderMenu menu = new TraderMenu(id, displayTitle, model.rows, contents, icons.get(model.icon));
        });
    }

    public void loadTraders() {
        TraderFile data = loadFile("traders.yml", TraderFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            if (!guis.containsKey(model.gui)) return;
            Component displayName = MiniMessage.miniMessage().deserialize(model.display);
            EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.fromString(model.entity));
            traders.put(id, new Trader(id, displayName, type, guis.get(model.gui)));
        });
    }

    private <T> T loadFile(String fileName, Class<T> clazz) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, false);

        Yaml yaml = new Yaml(new Constructor(clazz, new LoaderOptions()));
        try (InputStream in = new FileInputStream(file)) {
            return yaml.load(in);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading " + fileName + ": " + e.getMessage());
            return null;
        }
    }
    // Old Yaml config by Bukkit, it sucks ass
//    private void loadResources() {
//        YamlConfiguration config = loadConfig("resources.yml");
//        ConfigurationSection section = config.getConfigurationSection("items");
//        if (section == null) return;
//
//        for (String id : section.getKeys(false)) {
//            ConfigurationSection details = section.getConfigurationSection(id);
//            if (details == null) continue;
//
//            String display = details.getString("display");
//            Material material = Material.matchMaterial(details.getString("material", "BARRIER"));
//            List<String> loreLines = details.getStringList("lore");
//            String world = details.getString("world");
//            String region = details.getString("region");
//            ItemStack item = itemFactory.createItem(display, material, loreLines, world);
//
//            items.put("aurora:" + id.toLowerCase(), item);
//        }
//    }
//
//    private void loadGear() {
//        YamlConfiguration config = loadConfig("gear.yml");
//        ConfigurationSection section = config.getConfigurationSection("items");
//        if (section == null) return;
//
//        for (String id : section.getKeys(false)) {
//            String display = section.getString("display");
//            Material material = Material.matchMaterial(section.getString("material", "BARRIER"));
//            int tier = section.getInt("tier", 0);
//            String type = section.getString("type");
//            String world = section.getString("world");
//            List<String> loreLines = section.getStringList("lore");
//            List<Map<?, ?>> enchantList = section.getMapList("enchants");
//            ItemStack item = itemFactory.createGear(display, material, tier, type, world, loreLines, enchantList);
//
//            items.put("aurora:" + id.toLowerCase(), item);
//
//        }
//    }

//    private void loadBlocks() {
//        YamlConfiguration config = loadConfig("blocks.yml");
//        ConfigurationSection section = config.getConfigurationSection("blocks");
//        if (section == null) return;
//
//        for (String id : section.getKeys(false)) {
//            ConfigurationSection details = section.getConfigurationSection(id);
//            if (details == null) continue;
//
//            // Completely broken, pass configsection to itemfactory
//            Material material = Material.matchMaterial(details.getString("material", "STONE"));
//            List<Map<?, ?>> independentPool = details.getMapList("independent");
//            List<Map<?, ?>> choicePool = details.getMapList("choice");
//            // parsePool() for both here
//            blocks.put("aurora:" + id.toLowerCase(), new BlockData(material, List.of()));
//        }
//    }
//
//    private void loadIcons() {
//        YamlConfiguration config = loadConfig("icons.yml");
//        ConfigurationSection section = config.getConfigurationSection("icons");
//        if (section == null) return;
//
//        for (String id : section.getKeys(false)) {
//            ConfigurationSection details = section.getConfigurationSection(id);
//            // Icons are basically resources/simple items for menus
//            ItemStack icon = itemFactory.createItem(
//                    details.getString("display"),
//                    Material.matchMaterial(details.getString("material", "PAPER")),
//                    details.getStringList("lore"),
//                    null, null
//            );
//            icons.put(id.toLowerCase(), icon);
//        }
//    }
//
//    private void loadTraders() {
//        YamlConfiguration config = loadConfig("traders.yml");
//        ConfigurationSection section = config.getConfigurationSection("traders");
//        if (section == null) return;
//
//        for (String id : section.getKeys(false)) {
//            ConfigurationSection details = section.getConfigurationSection(id);
//            String guiId = details.getString("gui");
//
//            // Link the physical trader ID to the logic/entity type
//            // Trader object handles the entity type and the GUI reference
//            traders.put(id.toLowerCase(), new Trader(
//                    details.getString("display"),
//                    details.getString("entity", "VILLAGER"),
//                    guiId
//            ));
//        }
//    }

//    private void loadMobs() {
//        YamlConfiguration config = loadConfig("mobs.yml");
//        ConfigurationSection section = config.getConfigurationSection("mobs");
//        if (section == null) return;
//
//        for (String id : section.getKeys(false)) {
//            ConfigurationSection details = section.getConfigurationSection(id);
//
//            // Build the MobData (stats, equipment IDs, etc.)
//            MobData data = new MobData(
//                    details.getString("display"),
//                    details.getString("type", "ZOMBIE"),
//                    details.getInt("health", 20),
//                    details.getString("weapon"), // Just the ID string
//                    details.getString("armor_chest") // Just the ID string
//            );
//
//            mobs.put("aurora:" + id.toLowerCase(), data);
//        }
//    }
//
//    private YamlConfiguration loadConfig(String fileName) {
//        File file = new File(plugin.getDataFolder(), fileName);
//        if (!file.exists()) plugin.saveResource(fileName, false);
//        return YamlConfiguration.loadConfiguration(file);
//    }
}
