package org.charky.aurora.core.data;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.LootPool;
import org.charky.aurora.core.models.MobModel;
import org.charky.aurora.features.spawner.MobData;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

// Builds mobs from their models: equipment, attributes and drops. Gear they drop
// is also listed for the Armory
public class MobLoader {
    private final Logger logger;
    private final Map<String, ItemStack> items;
    private final LootPoolBuilder loot;
    private final Predicate<String> isResource;

    public MobLoader(Logger logger, Map<String, ItemStack> items, LootPoolBuilder loot, Predicate<String> isResource) {
        this.logger = logger;
        this.items = items;
        this.loot = loot;
        this.isResource = isResource;
    }

    public void load(Map<String, MobModel> data, Map<String, MobData> mobs, Map<String, Component> armory) {
        data.forEach((id, model) -> {
            Component displayName = MiniMessage.miniMessage().deserialize(model.display);

            // Every pool rolls, so a mob with both drop lists gets both, and a mob
            // with none simply drops nothing
            List<LootPool> pools = loot.build(model.independent, model.choices, true);
            LootPool drops = looting -> pools.stream()
                    .flatMap(pool -> pool.roll(looting).stream())
                    .toList();

            mobs.put(id.toLowerCase(), new MobData(id, EntityType.fromName(model.entity), displayName,
                    model.health, model.defence, model.attack, model.attack_cooldown,
                    equipment(model), attributes(id, model), drops));
            addArmoryDrops(model, displayName, armory);
        });
    }

    private Map<EquipmentSlot, ItemStack> equipment(MobModel model) {
        Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
        if (model.equip == null) return equipment;

        equip(equipment, EquipmentSlot.HEAD, model.equip.head);
        equip(equipment, EquipmentSlot.CHEST, model.equip.chest);
        equip(equipment, EquipmentSlot.LEGS, model.equip.legs);
        equip(equipment, EquipmentSlot.FEET, model.equip.feet);
        equip(equipment, EquipmentSlot.HAND, model.equip.hand);
        equip(equipment, EquipmentSlot.OFF_HAND, model.equip.off_hand);
        return equipment;
    }

    private void equip(Map<EquipmentSlot, ItemStack> equipment, EquipmentSlot slot, String itemId) {
        if (itemId != null && items.containsKey(itemId)) equipment.put(slot, items.get(itemId));
    }

    private Map<Attribute, Double> attributes(String id, MobModel model) {
        Map<Attribute, Double> attributes = new HashMap<>();
        if (model.attributes == null) return attributes;

        for (MobModel.AttributeEntry entry : model.attributes) {
            // Namespaced keys must be lowercase
            NamespacedKey key = NamespacedKey.fromString(entry.attr.toLowerCase());
            Attribute attribute = (key == null) ? null : Registry.ATTRIBUTE.get(key);
            if (attribute == null) {
                logger.warning("Mob '" + id + "': unknown attribute '" + entry.attr + "'");
                continue;
            }
            attributes.put(attribute, entry.value);
        }
        return attributes;
    }

    // Every gear drop is unlockable in the Armory, the first mob listed claims it
    private void addArmoryDrops(MobModel model, Component mobName, Map<String, Component> armory) {
        List<String> ids = new ArrayList<>();
        if (model.independent != null) model.independent.forEach(drop -> ids.add(drop.id));
        if (model.choices != null) model.choices.options.forEach(choice -> ids.add(choice.id));

        for (String id : ids) {
            if (items.containsKey(id) && !isResource.test(id)) armory.putIfAbsent(id, mobName);
        }
    }
}
