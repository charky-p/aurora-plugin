package org.charky.aurora.features.combat;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.features.spawner.MobData;

// What a mob's hit does to a player: hearts from the attack level against the
// player's protection, and the slow swing of heavy hitters
public final class MobDamage {
    // Defence on a carried shield counts as this many extra protection levels each
    private static final double DEFENCE_LEVELS = 1.0;

    private MobDamage() {
    }

    // In vanilla health points, one heart is two
    public static double dealt(MobData mob, Player player, boolean ranged) {
        double hearts = CombatFormulas.heartsPerHit(mob.attack(), protection(player)) * CombatFormulas.MOB_DAMAGE_SCALE;
        if (ranged) hearts *= CombatFormulas.RANGED_SCALE;
        return hearts * 2.0;
    }

    // Average protection over the four slots, plus Defence from the best shield
    // carried anywhere, held or not
    static double protection(Player player) {
        double armour = Enchants.average(player.getInventory().getArmorContents(), "protection");
        return armour + shieldDefence(player) * DEFENCE_LEVELS;
    }

    private static int shieldDefence(Player player) {
        int best = Enchants.level(player.getInventory().getItemInOffHand(), "defence");
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.SHIELD) {
                best = Math.max(best, Enchants.level(item, "defence"));
            }
        }
        return best;
    }

    // Heavy hitters swing every attack, only one per cooldown lands. The last
    // landed hit is kept on the mob itself, so nothing leaks when it dies
    public static boolean onCooldown(LivingEntity mob, double cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;

        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        long now = System.currentTimeMillis();
        long last = pdc.getOrDefault(DataKeys.LAST_ATTACK_KEY, PersistentDataType.LONG, 0L);
        if (now - last < cooldownSeconds * 1000) return true;

        pdc.set(DataKeys.LAST_ATTACK_KEY, PersistentDataType.LONG, now);
        return false;
    }
}
