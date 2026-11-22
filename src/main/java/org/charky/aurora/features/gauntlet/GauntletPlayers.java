package org.charky.aurora.features.gauntlet;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

// Health and hunger, set the same way by runs, duels and the PvP zone
final class GauntletPlayers {
    private GauntletPlayers() {
    }

    // Their most health, boons and talents counted; 20 without the attribute
    static double maxHealth(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        return max == null ? 20 : max.getValue();
    }

    // Full health and hunger, and no fire
    static void heal(Player player) {
        player.setHealth(maxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setFireTicks(0);
    }

    // Some health back, never past their most
    static void heal(Player player, double amount) {
        player.setHealth(Math.min(maxHealth(player), player.getHealth() + amount));
    }

    // No more health than their most, after it went down (a pact took hearts)
    static void capHealth(Player player) {
        double max = maxHealth(player);
        if (player.getHealth() > max) player.setHealth(max);
    }
}
