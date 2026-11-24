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

// What a mob's hit does to a player: hearts from the mob's attack level less the
// player's Defence, then a share taken off by their Protection, and the slow swing
// of heavy hitters
public final class MobDamage {

    private MobDamage() {
    }

    // In vanilla health points, one heart is two
    public static double dealt(MobData mob, Player player, boolean ranged) {
        double hearts = CombatFormulas.heartsPerHit(mob.attack(), defence(player))
                * CombatFormulas.protectionMultiplier(protection(player))
                * CombatFormulas.MOB_DAMAGE_SCALE * mob.damage();
        if (ranged) hearts *= CombatFormulas.RANGED_SCALE;
        return hearts * 2.0;
    }

    // Average Protection over the four armour slots
    static double protection(Player player) {
        return Enchants.average(player.getInventory().getArmorContents(), "protection");
    }

    // Defence adds up: every armour piece worn, and the best shield carried anywhere,
    // held or not
    static int defence(Player player) {
        int total = shieldDefence(player);
        for (ItemStack piece : player.getInventory().getArmorContents()) total += Enchants.level(piece, "defence");
        return total;
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
    // landed hit is kept on the mob itself, so nothing leaks when it dies. A breeze
    // is held to it when it shoots instead (CombatListener.onBreezeShoot)
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
