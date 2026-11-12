package org.charky.aurora.features.system;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

// The admin wand: left and right click a block for the two corners of a selection,
// right click a trader to remove it
public class WandListener implements Listener {
    private final NamespacedKey wandKey;
    private final WandManager wandManager;
    private final Aurora plugin;

    public WandListener(Aurora plugin) {
        this.wandKey = WandManager.wandKey(plugin);
        this.wandManager = plugin.getWandManager();
        this.plugin = plugin;
    }

    @EventHandler
    public void onSelect(PlayerInteractEvent event) {
        ItemStack wand = event.getItem();
        if (!isWand(wand)) return;

        // A wand never does what the blaze rod under it would
        event.setCancelled(true);
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            wandManager.setPos1(player.getUniqueId(), loc);
            TextUtils.send(player, "<aqua>Position 1 set to: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            wandManager.setPos2(player.getUniqueId(), loc);
            TextUtils.send(player, "<aqua>Position 2 set to: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
    }

    @EventHandler
    public void onWandEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isWand(item)) return;

        Entity target = event.getRightClicked();
        String traderId = target.getPersistentDataContainer().get(DataKeys.TRADER_KEY, PersistentDataType.STRING);
        if (traderId == null) return;

        event.setCancelled(true);

        Integer spotId = target.getPersistentDataContainer().get(DataKeys.TRADER_SPOT_KEY, PersistentDataType.INTEGER);

        // A trader from before the traders table has no spot, only the entity goes
        if (spotId != null) plugin.getTraderManager().removeSpot(spotId);
        target.remove();

        String spot = spotId == null ? "" : " <gray>(spot <white>#" + spotId + "<gray>)";
        TextUtils.send(player, "<red><b>ZAP!</b> <gray>Trader <white>" + traderId + spot + " <gray>has been removed.");
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2.0f);
    }

    private boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BOOLEAN);
    }
}
