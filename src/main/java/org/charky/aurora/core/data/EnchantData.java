package org.charky.aurora.core.data;

import org.bukkit.enchantments.Enchantment;

// An Aurora enchant, vanilla is null when it has no vanilla counterpart
public record EnchantData(String display, Enchantment vanilla) {
}
