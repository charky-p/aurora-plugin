package org.charky.aurora.core.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.core.models.GearModel;
import org.charky.aurora.core.models.IconModel;
import org.charky.aurora.core.models.ResourceModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Builds every ItemStack the registry hands out, from its YAML model
public class ItemFactory {
    private static final NamespacedKey NO_ARMOR_KEY = new NamespacedKey("aurora", "no_armor");

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ItemStack createResource(ResourceModel model) {
        ItemStack item = new ItemStack(Material.matchMaterial(model.material));
        item.editMeta(meta -> {
            meta.displayName(miniMessage.deserialize(model.display).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("💎 Resource").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            for (String line : model.lore) lore.add(miniMessage.deserialize(line));
            if (!model.lore.isEmpty()) lore.add(Component.empty());
            lore.add(world(model.world));

            hideFlags(meta);
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(false);
        });
        return item;
    }

    public ItemStack createGear(GearModel model, Map<String, EnchantData> enchants) {
        ItemStack item = new ItemStack(Material.matchMaterial(model.material));
        item.editMeta(meta -> {
            meta.displayName(gearName(model));

            List<Component> lore = new ArrayList<>();
            lore.add(type(model.type));
            lore.add(Component.empty());

            for (Map.Entry<String, Integer> entry : model.enchants.entrySet()) {
                EnchantData enchant = enchants.get(entry.getKey());
                lore.add(miniMessage.deserialize("<!italic><gray>»<white> " + enchant.display() + " " + entry.getValue()));
                if (enchant.vanilla() != null) meta.addEnchant(enchant.vanilla(), entry.getValue(), true);
                storeEnchant(meta, entry.getKey(), entry.getValue());
            }
            if (!model.enchants.isEmpty()) lore.add(Component.empty());

            for (String line : model.lore) lore.add(miniMessage.deserialize("<!italic>" + line));
            if (!model.lore.isEmpty()) lore.add(Component.empty());
            lore.add(world(model.world));

            removeArmorPoints(item, meta);
            makeRightTool(item, meta);

            if (!model.enchants.isEmpty()) meta.setEnchantmentGlintOverride(true);
            hideFlags(meta);
            meta.lore(lore);
        });
        return item;
    }

    public ItemStack createIcon(IconModel model) {
        ItemStack item = new ItemStack(Material.matchMaterial(model.material));
        item.editMeta(meta -> {
            meta.displayName(miniMessage.deserialize(model.display));
            List<Component> lore = new ArrayList<>();
            for (String line : model.lore) lore.add(miniMessage.deserialize(line));

            if (model.glow) meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.lore(lore);
        });
        return item;
    }

    private Component gearName(GearModel model) {
        Component name = miniMessage.deserialize(model.display).decoration(TextDecoration.ITALIC, false);
        if (model.tier == 0) return name;

        Component tierTag = Component.text(" [T" + model.tier + "]")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);
        return Component.empty().append(name).append(tierTag);
    }

    // Levels live in the item's PDC, lore is only display, see Enchants
    private static void storeEnchant(ItemMeta meta, String enchantId, int level) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        PersistentDataContainer enchantPdc = pdc.has(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER)
                ? pdc.get(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER)
                : pdc.getAdapterContext().newPersistentDataContainer();

        enchantPdc.set(new NamespacedKey("aurora", enchantId.toLowerCase()), PersistentDataType.INTEGER, level);
        pdc.set(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER, enchantPdc);
    }

    // Protection is the only defence, any modifier on an item replaces its
    // vanilla ones, so a zero armour modifier removes leather/iron/gold points
    private static void removeArmorPoints(ItemStack item, ItemMeta meta) {
        EquipmentSlot slot = item.getType().getEquipmentSlot();
        if (!slot.isArmor()) return;

        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                NO_ARMOR_KEY, 0, AttributeModifier.Operation.ADD_NUMBER, slot.getGroup()));
    }

    // Vanilla wood and gold tiers cannot harvest ores, so they dig them three
    // times slower. Our tools are the right tool for every block of their kind, at
    // their tier's own speed, so every upgrade digs faster. Picks mine pickaxe
    // blocks, axes the Verdant Groves logs. The rules are what MineListener checks
    // before Fortune counts, so a tool with both rules (a god tool) would do both
    private static void makeRightTool(ItemStack item, ItemMeta meta) {
        Tag<Material> blocks;
        if (Tag.ITEMS_PICKAXES.isTagged(item.getType())) blocks = Tag.MINEABLE_PICKAXE;
        else if (Tag.ITEMS_AXES.isTagged(item.getType())) blocks = Tag.MINEABLE_AXE;
        else return;

        ToolComponent tool = meta.getTool();
        tool.setRules(new ArrayList<>());
        tool.addRule(blocks, tierSpeed(item.getType()), true);
        meta.setTool(tool);
    }

    // Vanilla tier speeds, the same for every tool of a tier. Efficiency still adds
    // on top through the enchant
    private static float tierSpeed(Material material) {
        String name = material.name();
        if (name.startsWith("WOODEN_")) return 2f;
        if (name.startsWith("STONE_")) return 4f;
        if (name.startsWith("COPPER_")) return 5f;
        if (name.startsWith("IRON_")) return 6f;
        if (name.startsWith("DIAMOND_")) return 8f;
        if (name.startsWith("NETHERITE_")) return 9f;
        if (name.startsWith("GOLDEN_")) return 12f;
        return 1f;
    }

    private static Component type(String type) {
        String display = switch (type) {
            case "combat" -> "🗡 Combat";
            case "mining" -> "⛏ Mining";
            case "armor" -> "🛡 Armor";
            default -> "";
        };
        return Component.text(display).color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private static Component world(String world) {
        if (world.equals("overworld")) {
            return Component.text("Overworld 🌎")
                    .color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return Component.text("No world");
    }

    private static void hideFlags(ItemMeta meta) {
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
    }
}
