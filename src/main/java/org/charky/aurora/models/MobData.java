package org.charky.aurora.models;

import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public record MobData(
        String id,
        EntityType type,
        Component displayName,
        Map<EquipmentSlot, ItemStack> equipment,
        Map<Attribute, Double> attributes,
        List<ItemStack> drops
) {
    public void apply(org.bukkit.entity.LivingEntity entity) {
        entity.customName(this.displayName);
        entity.setCustomNameVisible(true);

        if (this.equipment != null) {
            this.equipment.forEach((slot, item) -> {
                entity.getEquipment().setItem(slot, item.clone());
            });
        }

        if (this.attributes != null) {
            this.attributes.forEach((attr, value) -> {
                var instance = entity.getAttribute(attr);
                if (instance != null) {
                    instance.setBaseValue(value);

                    if (attr == Attribute.MAX_HEALTH) {
                        entity.setHealth(value);
                    }
                }
            });
        }
    }
}
