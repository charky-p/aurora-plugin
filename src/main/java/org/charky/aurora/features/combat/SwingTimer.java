package org.charky.aurora.features.combat;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Vanilla resets the attack cooldown before firing the damage event, so
// getAttackCooldown() reads 0 there and every swing looks uncharged.
// Swings are timed here instead
public class SwingTimer {
    private final Map<UUID, Long> lastSwing = new HashMap<>();

    // How charged this swing is, and starts timing the next one
    public double swing(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastSwing.put(player.getUniqueId(), now);
        if (previous == null) return 1.0;

        return CombatFormulas.chargeMultiplier(now - previous);
    }

    public void forget(Player player) {
        lastSwing.remove(player.getUniqueId());
    }
}
