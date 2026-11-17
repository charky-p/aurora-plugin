package org.charky.aurora.features.combat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.features.spawner.MobData;

// What a player's hit does to a mob: Sharpness, Bloodlust, swing charge, a jump crit
// and a mace's smash, less the mob's defence
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
                * (critical ? CombatFormulas.CRIT_MULTIPLIER : 1.0)
                * (weapon.getType() == Material.MACE ? smash(attacker.getFallDistance()) : 1.0);

        // Never negative, or the mob heals past its max and cannot be killed
        double defence = victim == null ? 0.0 : victim.defence();
        return Math.max(0.0, Math.round(raw - defence));
    }

    // A mace hit while falling: 1 + 0.2 a block fallen, at most 3 times, on top of a crit
    public static double smash(double fallen) {
        if (fallen <= 1.5) return 1.0;
        return Math.min(3.0, 1.0 + 0.2 * fallen);
    }

    // The weapon's hit before anything situational
    public static double baseHit(ItemStack weapon) {
        return CombatFormulas.BASE_DAMAGE + CombatFormulas.sharpnessBonus(Enchants.level(weapon, "sharpness"));
    }
}
