package org.charky.aurora.features.spawner;

import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.LootPool;

import java.util.Map;

public record MobData(
        String id,
        EntityType type,
        Component displayName,
        double health,
        double defence,
        double attack,
        double attackCooldown,
        Map<EquipmentSlot, ItemStack> equipment,
        Map<Attribute, Double> attributes,
        LootPool drops
) {
}
