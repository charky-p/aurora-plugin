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
// the mob's attack level. A similar tier gets nothing through, a higher one some of
// the hit, and one far above breaks through in full and knocks the shield down for a
// while, like an axe. Blocked hits never knock the player back
public final class ShieldBlock {
    // As long as vanilla disables a shield hit by an axe
    private static final int BREAK_COOLDOWN_TICKS = 100;

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
    // so the shield can't stop it again and nothing is knocked back
    public static void take(Player player, MobData mob, double unblocked) {
        ItemStack shield = player.getActiveItem();
        int deflection = Enchants.level(shield, "deflection");
        double gap = CombatFormulas.deflectionGap(mob.attack(), deflection);

        if (CombatFormulas.breaksShield(gap)) {
            player.clearActiveItem();
            player.setCooldown(Material.SHIELD, BREAK_COOLDOWN_TICKS);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1f, 1f);
            player.sendActionBar(Component.text("Your shield was broken through", NamedTextColor.RED, TextDecoration.BOLD));
            player.damage(unblocked);
            return;
        }

        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
        double through = unblocked * CombatFormulas.throughShield(gap);
        if (through > 0) player.damage(through);
    }
}
