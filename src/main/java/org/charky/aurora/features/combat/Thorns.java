package org.charky.aurora.features.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.core.DataRegistry;

// Hurts the mob that hit the wearer by a set amount per level of average Thorns
// over the armour. What the wearer holds has nothing to do with it
public class Thorns {
    private static final double DAMAGE_PER_LEVEL = 20;

    private final DataRegistry registry;
    private final DamageIndicator indicator;

    public Thorns(DataRegistry registry, DamageIndicator indicator) {
        this.registry = registry;
        this.indicator = indicator;
    }

    public void reflect(Player player, LivingEntity source) {
        double thorns = Enchants.average(player.getInventory().getArmorContents(), "thorns");
        if (thorns <= 0) return;

        double reflected = Math.round(DAMAGE_PER_LEVEL * thorns);
        if (reflected <= 0) return;

        CustomHealth.apply(source, reflected, player, registry);
        indicator.show(source, reflected);
    }
}
