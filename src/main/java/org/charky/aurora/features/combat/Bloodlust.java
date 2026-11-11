package org.charky.aurora.features.combat;

import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;

// Each kill adds a stack for a few seconds, each stack is worth BONUS per enchant
// level, so Bloodlust 2 at full stacks is +30%. Stacks live on the player's PDC
public final class Bloodlust {
    private static final int MAX_STACKS = 3;
    private static final long DURATION_MS = 6000L;
    private static final double BONUS = 0.05;

    private Bloodlust() {
    }

    public static double multiplier(Player player, int level) {
        return 1 + activeStacks(player) * BONUS * level;
    }

    // A kill refreshes the timer and adds a stack, up to the cap
    public static void addStack(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        int stacks = Math.min(MAX_STACKS, activeStacks(player) + 1);
        pdc.set(DataKeys.BLOODLUST_STACKS_KEY, PersistentDataType.INTEGER, stacks);
        pdc.set(DataKeys.BLOODLUST_UNTIL_KEY, PersistentDataType.LONG, System.currentTimeMillis() + DURATION_MS);
    }

    private static int activeStacks(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        long until = pdc.getOrDefault(DataKeys.BLOODLUST_UNTIL_KEY, PersistentDataType.LONG, 0L);
        if (System.currentTimeMillis() > until) return 0;
        return pdc.getOrDefault(DataKeys.BLOODLUST_STACKS_KEY, PersistentDataType.INTEGER, 0);
    }
}
