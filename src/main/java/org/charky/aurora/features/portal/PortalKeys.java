package org.charky.aurora.features.portal;

import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.List;

// Key items from resources.csv (the unlocks column) give a player a tag when right
// clicked, locked portals only let players with that tag through. Tags are vanilla
// scoreboard tags, so they live in the player's data and `/tag <player> add <tag>` works
public final class PortalKeys {
    private PortalKeys() {
    }

    public static boolean has(Player player, String tag) {
        return player.getScoreboardTags().contains(tag);
    }

    // True when the key was used and one should be taken away
    public static boolean use(Player player, DataRegistry registry, String itemId) {
        String tag = registry.getUnlock(itemId);
        if (tag == null) return false;

        Component name = itemName(registry, itemId);
        if (has(player, tag)) {
            player.sendActionBar(TextUtils.parse("<gray>You have already used a ").append(name));
            return false;
        }

        player.addScoreboardTag(tag);
        player.sendMessage(TextUtils.parse("<green>Used ").append(name).append(TextUtils.parse("<green>, a new portal is open to you")));
        player.playSound(player.getLocation(), Sound.BLOCK_VAULT_OPEN_SHUTTER, 1f, 1f);
        return true;
    }

    // What to tell a player turned away, the item that opens it or the bare tag
    public static Component missing(DataRegistry registry, String tag) {
        List<String> keys = registry.getKeysFor(tag);
        if (keys.isEmpty()) return TextUtils.parse("<red>This portal is locked");
        return TextUtils.parse("<red>You need a ").append(itemName(registry, keys.getFirst()))
                .append(TextUtils.parse("<red> to enter"));
    }

    private static Component itemName(DataRegistry registry, String itemId) {
        ItemStack item = registry.getAllItems().get(itemId);
        Component name = item == null ? null : item.getItemMeta().displayName();
        return name == null ? Component.text(itemId) : name;
    }
}
