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
        // Multiplies every hit this mob lands, 1 leaves the attack level as it is
        double damage,
        // Looting below this drops none of its resources, gear and keys still drop
        int density,
        Map<EquipmentSlot, ItemStack> equipment,
        Map<Attribute, Double> attributes,
        // An outline seen through walls, from the glowing column
        boolean glowing,
        LootPool drops
) {
}
