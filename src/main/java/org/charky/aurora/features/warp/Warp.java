package org.charky.aurora.features.warp;

import org.bukkit.inventory.ItemStack;

// A region in the Warps menu, it leads to the spawn of its world. With a tag it is
// locked the same way that region's portal is, null for anyone
public record Warp(String id, ItemStack icon, String world, String tag) {
}
