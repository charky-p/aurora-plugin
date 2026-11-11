package org.charky.aurora.features.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.core.DataRegistry;

// Hands back a share of the wearer's own base hit to the mob that hit them,
// per level of average Thorns over the armour
public class Thorns {
    private static final double SHARE = 0.25;

    private final DataRegistry registry;
    private final DamageIndicator indicator;

    public Thorns(DataRegistry registry, DamageIndicator indicator) {
        this.registry = registry;
        this.indicator = indicator;
    }

    public void reflect(Player player, LivingEntity source) {
        double thorns = Enchants.average(player.getInventory().getArmorContents(), "thorns");
        if (thorns <= 0) return;

        double reflected = Math.round(PlayerDamage.baseHit(player.getInventory().getItemInMainHand()) * SHARE * thorns);
        if (reflected <= 0) return;

        CustomHealth.apply(source, reflected, player, registry);
        indicator.show(source, reflected);
    }
}
