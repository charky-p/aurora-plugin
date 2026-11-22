package org.charky.aurora.features.combat;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.features.tool.ToolManager;

// The bow, bought at the Dunes and upgraded with Power like the sword with Sharpness.
// A shot carries its Power and how far it was drawn on the arrow, and CombatListener
// prices the hit from them, not from what the player holds when it lands. Arrows are
// free for the Aurora bow: it keeps one arrow in the inventory to draw with and never
// uses it up. Gauntlet bows and crossbows use up real arrows, which come in care
// packages. Arrows fired cannot be picked up
public class BowListener implements Listener {
    public static final String BOW_ID = "bow";
    private static final NamespacedKey POWER_KEY = new NamespacedKey("aurora", "arrow_power");
    private static final NamespacedKey FORCE_KEY = new NamespacedKey("aurora", "arrow_force");
    // On every piece of gauntlet gear, its gear id (see GauntletGear)
    public static final NamespacedKey GAUNTLET_KEY = new NamespacedKey("aurora", "gauntlet_gear");

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isBow(event.getBow())) return;

        // Gauntlet bows use up real arrows, from care packages
        if (!isGauntlet(event.getBow())) {
            event.setConsumeItem(false);
            player.updateInventory();
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;

        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        pdc.set(POWER_KEY, PersistentDataType.INTEGER, Enchants.level(event.getBow(), "power"));
        pdc.set(FORCE_KEY, PersistentDataType.FLOAT, event.getForce());
    }

    // Vanilla only draws a bow with an arrow to fire, so one is handed over whenever
    // the bow comes out or is used without one
    @EventHandler
    public void onHold(PlayerItemHeldEvent event) {
        ItemStack held = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (isBow(held) && !isGauntlet(held)) keepArrow(event.getPlayer());
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (isBow(event.getItem()) && !isGauntlet(event.getItem())) keepArrow(event.getPlayer());
    }

    private static void keepArrow(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (!inventory.contains(Material.ARROW)) inventory.addItem(new ItemStack(Material.ARROW));
    }

    // The Aurora bow, or a gauntlet bow or crossbow
    public static boolean isBow(ItemStack item) {
        if (BOW_ID.equals(ToolManager.toolId(item))) return true;
        return item != null && (item.getType() == Material.BOW || item.getType() == Material.CROSSBOW) && isGauntlet(item);
    }

    private static boolean isGauntlet(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(GAUNTLET_KEY, PersistentDataType.STRING);
    }

    // A gauntlet trident thrown by a player is priced like an arrow, from its Power at full draw
    @EventHandler(ignoreCancelled = true)
    public void onThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident) || !(trident.getShooter() instanceof Player)) return;
        ItemStack item = trident.getItemStack();
        if (!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(GAUNTLET_KEY, PersistentDataType.STRING)) return;
        PersistentDataContainer pdc = trident.getPersistentDataContainer();
        pdc.set(POWER_KEY, PersistentDataType.INTEGER, Enchants.level(item, "power"));
        pdc.set(FORCE_KEY, PersistentDataType.FLOAT, 1f);
    }

    // A shot from the Aurora bow, null for any other projectile
    public static Shot shotOf(Entity projectile) {
        if (!(projectile instanceof AbstractArrow arrow)) return null;
        PersistentDataContainer pdc = arrow.getPersistentDataContainer();
        Integer power = pdc.get(POWER_KEY, PersistentDataType.INTEGER);
        Float force = pdc.get(FORCE_KEY, PersistentDataType.FLOAT);
        return power == null || force == null ? null : new Shot(power, force);
    }

    public record Shot(int power, float force) {
        // 10 plus Power in Sharpness's brackets, from a fifth for a flick of the bow to
        // all of it fully drawn, squared like a sword swing's charge
        public double raw() {
            double draw = Math.clamp(force, 0f, 1f);
            return (CombatFormulas.BASE_DAMAGE + CombatFormulas.sharpnessBonus(power)) * (0.2 + 0.8 * draw * draw);
        }
    }
}
