package org.charky.aurora.features.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;

// The Backpack item every player carries: a bundle that opens the backpack menu,
// the same as /backpack. It is only a button, it never holds anything itself
public class BackpackItemListener implements Listener {
    // In gear_other.csv, so its name and lore are data like any other item
    public static final String ITEM_ID = "starter_backpack";
    // Last hotbar slot, out of the way of the pick and sword
    private static final int PREFERRED_SLOT = 8;

    private final Aurora plugin;
    private final DataRegistry registry;
    private final BackpackManager backpacks;

    public BackpackItemListener(Aurora plugin) {
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
        this.backpacks = plugin.getBackpackManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        giveIfMissing(event.getPlayer());
    }

    // Next tick, the inventory is not the respawned one yet during the event
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) giveIfMissing(player);
        });
    }

    // A full inventory gets it on the next join instead
    private void giveIfMissing(Player player) {
        ItemStack template = registry.getAllItems().get(ITEM_ID);
        if (template == null) return;

        PlayerInventory inventory = player.getInventory();
        for (ItemStack slot : inventory.getContents()) {
            if (isBackpack(slot)) return;
        }

        ItemStack held = inventory.getItem(PREFERRED_SLOT);
        if (held == null || held.getType().isAir()) inventory.setItem(PREFERRED_SLOT, template.clone());
        else inventory.addItem(template.clone());
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isBackpack(event.getItem())) return;

        // Or vanilla tries to empty the bundle onto the ground
        event.setCancelled(true);
        backpacks.open(event.getPlayer());
    }

    // Right clicking it in the inventory opens the backpack too. Any click that
    // would put an item into the bundle is refused, vanilla bundles swallow whatever
    // they are clicked with or onto
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean onBackpack = isBackpack(clicked);
        boolean holding = isBackpack(cursor);
        if (!onBackpack && !holding) return;

        if (onBackpack && event.isRightClick() && isEmpty(cursor) && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            // Next tick, a menu can't be opened from inside a click
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) backpacks.open(player);
            });
            return;
        }

        if ((onBackpack && !isEmpty(cursor)) || (holding && !isEmpty(clicked))) event.setCancelled(true);
    }

    // It can't be thrown away, there is no shop that sells another
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isBackpack(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    private static boolean isBackpack(ItemStack item) {
        return ITEM_ID.equals(DataRegistry.itemId(item));
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
