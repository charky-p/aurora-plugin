package org.charky.aurora.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.loot.CumulativeLootPool;
import org.charky.aurora.common.loot.IndependentLootPool;
import org.charky.aurora.common.loot.ItemDrop;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.core.models.*;
import org.charky.aurora.features.player.BackpackManager;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.trader.*;
import org.charky.aurora.common.loot.LootPool;
import org.charky.aurora.features.trader.TraderNode;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.ItemFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class DataRegistry {
    private final Aurora plugin;
    private final ItemFactory itemFactory;
    private BackpackManager backpackManager;
    private record BlockData(Material material, int density, List<LootPool> pools) {}
    private record EnchantData(String display, Enchantment enchant) {}
    private final Map<String, ItemStack> items = new HashMap<>(); // resources + gear
    private final Map<String, EnchantData> enchants = new HashMap<>();
    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Map<String, MobData> mobs = new HashMap<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final Map<String, TraderMenu> guis = new HashMap<>();
    private final Map<String, Trader> traders = new HashMap<>();

    private final List<MenuItem> resourceList = new ArrayList<>();

    public DataRegistry(Aurora plugin) {
        this.plugin = plugin;
        this.itemFactory = plugin.getItemFactory();
        this.backpackManager = plugin.getBackpackManager();
    }

    public void setBackpackManager(BackpackManager backpackManager) {
        this.backpackManager = backpackManager;
    }

    public void loadAll() {
        items.clear();
        enchants.clear();
        blocks.clear();
        mobs.clear();
        icons.clear();
        guis.clear();
        traders.clear();
        resourceList.clear();

        loadResources();
        loadEnchants();
        loadGear("picks.yml", "mining");
        loadGear("armor.yml", "armor");
        loadGear("weapons.yml", "combat");
        loadGear("other.yml", "");
        loadBlocks();
        loadMobs("gormak.yml");
        loadMobs("origin.yml");
        loadIcons();
        // TODO: order matters since there is a hierarchy of GUIs, make this more robust
//        loadGuis("armor.yml");
//        loadGuis("compressors.yml");
        loadGuis("tools.yml");
        loadGuis("shops.yml");
        loadTraders();

         debugRegistry();
    }

    public List<String> getBlocks(String prefix) {
        return blocks.keySet()
                .stream()
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

    public List<MenuItem> getBlockDrops(String id, int fortune) {
        if (!hasBlock(id)) return new ArrayList<>();

        BlockData block = blocks.get(id);
        return blocks.get(id).pools().stream()
                    .map(LootPool::roll)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .peek(item -> item.setAmount(item.getAmount() * Math.max(0, fortune + 1 - block.density)))
                    .toList();
    }

    public boolean hasTrader(String id) {
        return traders.containsKey(id);
    }

    public TraderMenu getGui(String id) {
        return guis.get(id);
    }

    public Trader getTrader(String id) {
        return traders.get(id);
    }

    public boolean hasMob(String id) {
        return mobs.containsKey(id);
    }

    public MobData getMob(String id) {
        return mobs.get(id);
    }

    public List<MenuItem> getMobDrops(String id) {
        return mobs.get(id).drops().roll();
    }

    public List<String> getMobs() {
        return new ArrayList<>(mobs.keySet());
    }

    public List<MenuItem> getItems() {
        return resourceList;
    }

    public Map<String, ItemStack> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    public Map<String, ItemStack> getAllIcons() {
        return Collections.unmodifiableMap(icons);
    }

    public MenuItem getItem(String item) {
        return new MenuItem(item, items.get(item));
    }

    private void debugRegistry() {
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

    private void loadResources() {
        Map<String, ResourceModel> data = loadYaml("resources.yml", ResourceModel.class);

        data.forEach((id, model) -> {
            ItemStack item = itemFactory.createItem(
                    model.display,
                    Material.matchMaterial(model.material),
                    model.lore,
                    model.world,
                    false
            );

            items.put(id.toLowerCase(), item);
            resourceList.add(new MenuItem(id.toLowerCase(), item));
        });
    }

    private void loadEnchants() {
        Map<String, EnchantModel> data = loadYaml("enchants.yml", EnchantModel.class);

        data.forEach((id, model) -> {
            if (model.enchant == null) enchants.put(id, new EnchantData(model.display, null));
            else enchants.put(id, new EnchantData(model.display, Registry.ENCHANTMENT.get(NamespacedKey.minecraft(model.enchant.toLowerCase()))));
        });
    }

    private void loadGear(String file, String defaultType) {
        Map<String, GearModel> data = loadYaml("gear/" + file, GearModel.class);

        data.forEach((id, model) -> {
            ItemStack item = new ItemStack(Material.matchMaterial(model.material));
            item.editMeta(meta -> {
                Component displayComponent = MiniMessage.miniMessage().deserialize(model.display).decoration(TextDecoration.ITALIC, false);
                if (model.tier == 0) meta.displayName(displayComponent);
                else {
                    Component tierTag = Component.text(" [T" + model.tier + "]")
                            .color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, false)
                            .decoration(TextDecoration.ITALIC, false);
                    meta.displayName(Component.empty().append(displayComponent).append(tierTag));
                }

                List<Component> lore = new ArrayList<>();
//                if (model.type == null) lore.add(parseType(defaultType));
                lore.add(parseType(model.type));
                lore.add(Component.empty());

                // Enchants
                for (Map.Entry<String, Integer> entry : model.enchants.entrySet()) {
                    EnchantData enchantData = enchants.get(entry.getKey());
                    lore.add(MiniMessage.miniMessage().deserialize("<!italic><gray>»<white> " + enchantData.display + " " + entry.getValue()));
                    if (enchantData.enchant != null) {
                        meta.addEnchant(enchantData.enchant, entry.getValue(), true);
                    }

                    // Add persistent data container enchant key
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    PersistentDataContainer enchantPdc = pdc.getAdapterContext().newPersistentDataContainer();

                    if (pdc.has(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER)) {
                        enchantPdc = pdc.get(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER);
                    }

                    enchantPdc.set(new NamespacedKey("aurora", entry.getKey().toLowerCase()), PersistentDataType.INTEGER, entry.getValue());
                    pdc.set(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER, enchantPdc);
                }

                if (!model.enchants.isEmpty()) lore.add(Component.empty());

                for (String line : model.lore) lore.add(MiniMessage.miniMessage().deserialize("<!italic>" + line));
                if (!model.lore.isEmpty()) lore.add(Component.empty());
                lore.add(parseWorld(model.world));
                meta.setUnbreakable(true);
                if (!model.enchants.isEmpty()) meta.setEnchantmentGlintOverride(true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                meta.lore(lore);
            });

            items.put(id.toLowerCase(), item);
        });
    }

    // TODO: TextUtils ngl
    private Component parseType(String type) {
        String display = switch (type) {
            case "combat" -> "\uD83D\uDDE1 Combat";
            case "mining" -> "⛏ Mining";
            case "armor" -> "\uD83D\uDEE1 Armor";
            default -> "";
        };

        return Component.text(display).color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private Component parseWorld(String world) {
        if (world.equals("overworld")) {
            return Component.text("Overworld \uD83C\uDF0E")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return Component.text("No world");
    }

    private void loadBlocks() {
        Map<String, BlockModel> data = loadYaml("blocks.yml", BlockModel.class);

        data.forEach((id, model) -> {
            Material material = Material.matchMaterial(model.material);

            List<LootPool> pools = new ArrayList<>();
            if (!model.independent.isEmpty()) {
                IndependentLootPool pool = new IndependentLootPool();
                for (BlockModel.IndependentPoolModel choice : model.independent) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(new MenuItem(choice.id, items.get(choice.id)), choice.min, choice.max), choice.chance);
                }
                pools.add(pool);
            }

            if (model.choices != null) {
                CumulativeLootPool pool = new CumulativeLootPool(model.choices.chance);
                for (BlockModel.ChoiceModel choice : model.choices.options) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(new MenuItem(choice.id, items.get(choice.id)), choice.min, choice.max), choice.weight);
                }
                pools.add(pool);
            }

            blocks.put(id.toLowerCase(), new BlockData(material, model.density, pools));
        });
    }

    private void loadMobs(String file) {
        Map<String, MobModel> data = loadYaml("mobs/" + file, MobModel.class);

        data.forEach((id, model) -> {
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
                    // Namespaced keys must be lowercase
                    NamespacedKey key = NamespacedKey.fromString(entry.attr.toLowerCase());
                    Attribute attribute = (key == null) ? null : Registry.ATTRIBUTE.get(key);
                    if (attribute == null) {
                        plugin.getLogger().warning("Mob '" + id + "': unknown attribute '" + entry.attr + "'");
                        continue;
                    }
                    attributes.put(attribute, entry.value);
                }
            }

            List<LootPool> pools = new ArrayList<>();
            if (!model.independent.isEmpty()) {
                IndependentLootPool pool = new IndependentLootPool();
                for (BlockModel.IndependentPoolModel choice : model.independent) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(new MenuItem(choice.id, items.get(choice.id)), choice.min, choice.max), choice.chance);
                }
                pools.add(pool);
            }

            if (model.choices != null) {
                CumulativeLootPool pool = new CumulativeLootPool(model.choices.chance);
                for (BlockModel.ChoiceModel choice : model.choices.options) {
                    if (!items.containsKey(choice.id)) continue;

                    pool.add(new ItemDrop(new MenuItem(choice.id, items.get(choice.id)), choice.min, choice.max), choice.weight);
                }
                pools.add(pool);
            }

            // TODO: Not implemented with multiple pools, only one
            MobData mobData = new MobData(id, type, displayName, model.health, model.defence, model.attack, equipment, attributes, pools.getFirst());
            mobs.put(id.toLowerCase(), mobData);
        });
    }

    private void loadIcons() {
        Map<String, IconModel> data = loadYaml("icons.yml", IconModel.class);

        data.forEach((id, model) -> {
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

    private void loadGuis(String file) {
        Map<String, GuiModel> data = loadYaml("guis/" + file, GuiModel.class);

        // Make map for look up existence
        data.forEach((id, model) -> {
            Component title = MiniMessage.miniMessage().deserialize(model.title);
            ItemStack icon = icons.get(model.icon);
            guis.put(id, new TraderMenu(id, title, model.rows, new HashMap<>(), icon));
        });

        data.forEach((id, model) -> {
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
                        if (component == null) {
                            plugin.getLogger().severe("GUI ERROR: Character '" + rowStr.charAt(col) + "' in layout of '" + id + "' is missing from components!");
                            contents.put(row * 9 + col, new TraderBorder());
                            continue; // Skip to next character instead of crashing
                        }

                        if (component.type.equals("GUI")) {
                            // should never be called unless error with yaml
                            if (!guis.containsKey(component.id.toLowerCase())) {
                                contents.put(row * 9 + col, new TraderBorder());
                                continue;
                            }
                            contents.put(row * 9 + col, guis.get(component.id.toLowerCase()));
                        } else {
                            // Purchasable item
                            // Error happened with yaml, TODO: better error handling with yaml
                            if (!items.containsKey(component.id.toLowerCase())) {
                                contents.put(row * 9 + col, new TraderBorder());
                                continue;
                            }
                            MenuItem menu = new MenuItem(component.id.toLowerCase(), items.get(component.id.toLowerCase()), component.amount);
                            List<MenuItem> costs = new ArrayList<>();
                            for (GuiModel.CostEntry entry : component.cost) {
                                if (!items.containsKey(entry.id.toLowerCase())) continue;
                                costs.add(new MenuItem(entry.id.toLowerCase(), items.get(entry.id.toLowerCase()), entry.amount));
                            }
                            contents.put(row * 9 + col, new TraderItem(backpackManager, component.id.toLowerCase(), menu, costs));
                        }
                    }
                }
            }

            TraderMenu menu = guis.get(id);
            if (menu != null) menu.setContents(contents);
//            TraderMenu menu = new TraderMenu(id, displayTitle, model.rows, contents, icons.get(model.icon));
//            guis.put(id, menu);
        });
    }

    private void loadTraders() {
        Map<String, TraderModel> data = loadYaml("traders.yml", TraderModel.class);

        data.forEach((id, model) -> {
            Component displayName = MiniMessage.miniMessage().deserialize(model.display);

            NamespacedKey key = NamespacedKey.fromString("minecraft:" + model.entity.toLowerCase());
            EntityType type = (key != null) ? Registry.ENTITY_TYPE.get(key) : null;

            if (type == null) {
                plugin.getLogger().warning("Invalid EntityType '" + model.entity + "' for trader: " + id);
                type = EntityType.VILLAGER; // Default fallback so it doesn't crash
            }
            traders.put(id, new Trader(DataKeys.TRADER_KEY, id, displayName, type, guis.get(model.gui)));
        });
    }

    private <T> Map<String, T> loadYaml(String fileName, Class<T> modelClass) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, false);

        Yaml yaml = new Yaml();
        try (InputStream in = new FileInputStream(file)) {
            // Load YAML items map
            Map<String, Object> root = yaml.load(in);
            if (root == null || !root.containsKey("items")) return Collections.emptyMap();

            Map<String, Object> itemsMap = (Map<String, Object>) root.get("items");
            // TODO: linked hash map is to ensure order (need to specify order correctly)
            Map<String, T> result = new LinkedHashMap<>();

            // Convert map into model class with SnakeYAML
            Yaml modelMapper = new Yaml(new Constructor(modelClass, new LoaderOptions()));
            for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                T model = modelMapper.load(yaml.dump(entry.getValue()));
                result.put(entry.getKey(), model);
            }

            return result;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load " + fileName + ": " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
