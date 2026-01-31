package org.charky.aurora;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
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
import org.charky.aurora.models.*;
import org.charky.aurora.util.ItemFactory;
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
    private record BlockData(Material material, List<LootPool> pools) {}
    private record EnchantData(String display, Enchantment enchant) {}
    private final Map<String, ItemStack> items = new HashMap<>(); // resources + gear
    private final Map<String, EnchantData> enchants = new HashMap<>();
    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Map<String, MobData> mobs = new HashMap<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final Map<String, TraderMenu> guis = new HashMap<>();
    private final Map<String, Trader> traders = new HashMap<>();

    public DataRegistry(Aurora plugin) {
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
        loadEnchants();
        loadGear();
        loadBlocks();
        loadMobs();
        loadIcons();
        loadGuis();
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

    public List<MenuItem> getBlockDrops(String id) {
        if (!hasBlock(id)) return new ArrayList<>();
        return blocks.get(id).pools().stream()
                    .map(LootPool::roll)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
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
        ResourceFile data = loadFile("resources.yml", ResourceFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            ItemStack item = itemFactory.createItem(
                    model.display,
                    Material.matchMaterial(model.material),
                    model.lore,
                    model.world
            );

            items.put(id.toLowerCase(), item);
        });
    }

    private void loadEnchants() {
        EnchantFile data = loadFile("enchants.yml", EnchantFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            if (model.enchant == null) enchants.put(id, new EnchantData(model.display, null));
            else enchants.put(id, new EnchantData(model.display, Registry.ENCHANTMENT.get(NamespacedKey.minecraft(model.enchant.toLowerCase()))));
        });
    }

    private void loadGear() {
        GearFile data = loadFile("gear.yml", GearFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
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
                lore.add(parseType(model.type));
                lore.add(Component.empty());

                // Enchants
                for (Map.Entry<String, Integer> entry : model.enchants.entrySet()) {
                    EnchantData enchantData = enchants.get(entry.getKey());
                    lore.add(MiniMessage.miniMessage().deserialize("<!italic><gray>»<white> " + enchantData.display + " " + entry.getValue()));
                    if (enchantData.enchant != null) {
                        meta.addEnchant(enchantData.enchant, entry.getValue(), true);
                    }

                }

                if (!model.enchants.isEmpty()) lore.add(Component.empty());

                for (String line : model.lore) lore.add(MiniMessage.miniMessage().deserialize("<!italic>" + line));
                if (!model.lore.isEmpty()) lore.add(Component.empty());
                lore.add(parseWorld(model.world));
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                meta.lore(lore);
            });

            items.put(id.toLowerCase(), item);
        });
    }

    private Component parseType(String type) {
        String display = switch (type) {
            case "combat" -> "\uD83D\uDDE1 Combat";
            case "mining" -> "⛏ Mining";
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

            blocks.put(id.toLowerCase(), new BlockData(material, pools));
        });
    }

    private void loadMobs() {
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
            mobs.put(id.toLowerCase(), mobData);
        });
    }

    private void loadIcons() {
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

    private void loadGuis() {
        GuiFile data = loadFile("guis.yml", GuiFile.class);
        if (data == null || data.items == null) return;

        // Make map for look up existence
        data.items.forEach((id, model) -> {
            Component title = MiniMessage.miniMessage().deserialize(model.title);
            ItemStack icon = icons.get(model.icon);
            guis.put(id, new TraderMenu(id, title, model.rows, new HashMap<>(), icon));
        });

        data.items.forEach((id, model) -> {
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

            TraderMenu menu = guis.get(id);
            if (menu != null) menu.setContents(contents);
//            TraderMenu menu = new TraderMenu(id, displayTitle, model.rows, contents, icons.get(model.icon));
//            guis.put(id, menu);
        });
    }

    private void loadTraders() {
        TraderFile data = loadFile("traders.yml", TraderFile.class);
        if (data == null || data.items == null) return;

        data.items.forEach((id, model) -> {
            Component displayName = MiniMessage.miniMessage().deserialize(model.display);

            String entityId = model.entity.toLowerCase();
            NamespacedKey key = NamespacedKey.fromString("minecraft:" + entityId);
            EntityType type = (key != null) ? Registry.ENTITY_TYPE.get(key) : null;

            if (type == null) {
                plugin.getLogger().warning("Invalid EntityType '" + model.entity + "' for trader: " + id);
                type = EntityType.VILLAGER; // Default fallback so it doesn't crash
            }
            traders.put(id, new Trader(id, displayName, type, guis.get(model.gui)));
        });
    }

    private <T> T loadFile(String fileName, Class<T> clazz) {
        File file = new File(plugin.getDataFolder(), fileName);
        plugin.saveResource(fileName, true);

        Yaml yaml = new Yaml(new Constructor(clazz, new LoaderOptions()));
        try (InputStream in = new FileInputStream(file)) {
            return yaml.load(in);
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}
