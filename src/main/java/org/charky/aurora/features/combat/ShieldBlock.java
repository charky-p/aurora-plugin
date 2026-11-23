package org.charky.aurora.features.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.features.spawner.MobData;

// A raised shield is extra armour against mob hits, set by its Deflection against
// the mob's attack level. Up to two tiers (zones) above what it covers gets nothing
// through, higher some of the hit, and far above breaks through in full and knocks the
// shield down for a while, like an axe. Blocked hits never knock the player back, and
// one the shield held deflects a share of the holder's own hit back, like thorns
public final class ShieldBlock {
    // As long as vanilla disables a shield hit by an axe
    private static final int BREAK_COOLDOWN_TICKS = 100;
    // Share of the holder's own hit a blocked hit sends back at its attacker
    public static final double DEFLECT_SHARE = 0.3;

    private ShieldBlock() {
    }

    // Raised, and facing what the hit came from, the way vanilla decides a block
    public static boolean blocks(Player player, Entity damager) {
        if (!player.isBlocking()) return false;

        Location from = damager.getLocation();
        Vector toSource = from.toVector().subtract(player.getLocation().toVector()).setY(0);
        Vector facing = player.getLocation().getDirection().setY(0);
        if (toSource.lengthSquared() == 0 || facing.lengthSquared() == 0) return true;
        return toSource.normalize().dot(facing.normalize()) > 0;
    }

    // The hit the vanilla event would have dealt, in health points. The event is
    // cancelled by the caller, so this deals whatever gets through itself. No source,
    // so the shield can't stop it again and nothing is knocked back. False if it broke
    // through the shield
    public static boolean take(Player player, MobData mob, double unblocked) {
        ItemStack shield = player.getActiveItem();
        int deflection = Enchants.level(shield, "deflection");
        double gap = CombatFormulas.deflectionGap(mob.attack(), deflection);

        if (CombatFormulas.breaksShield(gap)) {
            player.clearActiveItem();
            player.setCooldown(Material.SHIELD, BREAK_COOLDOWN_TICKS);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
            player.sendActionBar(Component.text("Your shield was broken through", NamedTextColor.RED, TextDecoration.BOLD));
            player.damage(unblocked);
            return false;
        }

        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        double through = unblocked * CombatFormulas.throughShield(gap);
        if (through > 0) player.damage(through);
        return true;
    }

    // What a blocked hit sends back at a mob: a share of the holder's own hit, from the
    // Sharpness or Power of what is in their hand
    public static double deflected(ItemStack weapon) {
        int stat = Math.max(Enchants.level(weapon, "sharpness"), Enchants.level(weapon, "power"));
        return Math.round(DEFLECT_SHARE * (CombatFormulas.BASE_DAMAGE + CombatFormulas.sharpnessBonus(stat)));
    }
}
