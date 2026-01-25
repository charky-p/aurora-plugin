package org.charky.aurora.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class WeaponListener implements Listener {
    @EventHandler
    public void onWeaponUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 1. Check if they are right-clicking with your custom weapon
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Gravity Blade")) return;

        // 2. Get the enchantment level for strength scaling
        int level = item.getEnchantmentLevel(Enchantment.KNOCKBACK); // Using Knockback as a placeholder for "Pull"
        double strength = 0.2 + (level * 0.1); // Base strength + 0.1 per level

        // 3. Find entities in a 5-block radius
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {

                // 4. Calculate the Direction Vector (Me - Mob)
                Vector pullDirection = player.getLocation().toVector().subtract(entity.getLocation().toVector());

                // 5. Normalize and apply strength
                // We use .normalize() so the pull is consistent regardless of distance
                Vector velocity = pullDirection.normalize().multiply(strength);

                // 6. Give the mob a little "hop" so they don't get stuck on the floor friction
                velocity.setY(0.2);

                entity.setVelocity(velocity);
            }
        }

        // Play a "Whirr" sound to make it feel like a vacuum
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.5f);
    }
}
