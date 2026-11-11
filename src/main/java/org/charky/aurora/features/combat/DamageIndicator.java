package org.charky.aurora.features.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

// The floating damage number that rises off a mob when it is hit
public class DamageIndicator {
    // Ticks the number lives for
    private static final int LIFETIME = 16;

    private final Plugin plugin;

    public DamageIndicator(Plugin plugin) {
        this.plugin = plugin;
    }

    public void show(LivingEntity victim, double amount) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Jitter so rapid hits do not stack into an unreadable blob
        Location location = victim.getEyeLocation().add(
                random.nextDouble(-0.4, 0.4),
                random.nextDouble(-0.2, 0.3),
                random.nextDouble(-0.4, 0.4));

        TextDisplay display = victim.getWorld().spawn(location, TextDisplay.class, text -> {
            NamedTextColor colour = amount > 0 ? NamedTextColor.RED : NamedTextColor.GRAY;

            text.text(Component.text("-" + (long) amount, colour, TextDecoration.BOLD));
            text.setBillboard(Display.Billboard.CENTER);
            text.setDefaultBackground(false);
            text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            text.setShadowed(true);
            text.setViewRange(0.4f);

            // Never persistent, or a stop mid-animation writes these into the
            // chunk, and they accumulate the way the husks did
            text.setPersistent(false);
        });

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= LIFETIME || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                display.teleport(display.getLocation().add(0, 0.04, 0));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
