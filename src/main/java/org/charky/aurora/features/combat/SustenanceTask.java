package org.charky.aurora.features.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.Enchants;

// Sustenance slowly restores hunger, one point every SECONDS_PER_POINT divided by
// the average Sustenance on the armour. A full Sustenance 1 set feeds a point every
// 30 s, Sustenance 5 every 6 s. Progress is kept on the player so nothing leaks
public class SustenanceTask extends BukkitRunnable {
    private static final double SECONDS_PER_POINT = 30.0;
    private static final long PERIOD_TICKS = 20L;
    private static final int MAX_FOOD = 20;

    public static void start(Plugin plugin) {
        new SustenanceTask().runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            double level = Enchants.average(player.getInventory().getArmorContents(), "sustenance");
            if (level <= 0 || player.getFoodLevel() >= MAX_FOOD) continue;

            PersistentDataContainer pdc = player.getPersistentDataContainer();
            double progress = pdc.getOrDefault(DataKeys.SUSTENANCE_KEY, PersistentDataType.DOUBLE, 0.0)
                    + level * (PERIOD_TICKS / 20.0) / SECONDS_PER_POINT;

            if (progress >= 1.0) {
                player.setFoodLevel(Math.min(MAX_FOOD, player.getFoodLevel() + 1));
                progress -= 1.0;
            }

            pdc.set(DataKeys.SUSTENANCE_KEY, PersistentDataType.DOUBLE, progress);
        }
    }
}
