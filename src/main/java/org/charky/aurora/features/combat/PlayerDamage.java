package org.charky.aurora.features.combat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.features.spawner.MobData;

// What a player's hit does to a mob: Sharpness, Bloodlust, swing charge and a
// jump crit, less the mob's defence
public class PlayerDamage {
    private final SwingTimer swings;

    public PlayerDamage(SwingTimer swings) {
        this.swings = swings;
    }

    public double dealt(Player attacker, MobData victim, boolean critical) {
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        double raw = baseHit(weapon)
                * Bloodlust.multiplier(attacker, Enchants.level(weapon, "bloodlust"))
                * swings.swing(attacker)
                * (critical ? CombatFormulas.CRIT_MULTIPLIER : 1.0);

        // Never negative, or the mob heals past its max and cannot be killed
        double defence = victim == null ? 0.0 : victim.defence();
        return Math.max(0.0, Math.round(raw - defence));
    }

    // The weapon's hit before anything situational, Thorns pays back a share of it
    public static double baseHit(ItemStack weapon) {
        return CombatFormulas.BASE_DAMAGE + CombatFormulas.sharpnessBonus(Enchants.level(weapon, "sharpness"));
    }
}
