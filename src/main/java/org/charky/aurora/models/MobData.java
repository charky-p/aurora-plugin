package org.charky.aurora.models;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public record MobData(
        String name,
        EntityType type,
        String displayName,
        ItemStack[] equipment,
        Map<Attribute, Double> attributes
) {}
