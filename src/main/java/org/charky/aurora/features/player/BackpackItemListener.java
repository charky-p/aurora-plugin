package org.charky.aurora.features.player;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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

// The Backpack item every player carries, it opens the backpack menu the same as
// /backpack. It only looks like a bundle: a real one swallows whatever it is clicked
// with and the client predicts that, so it is plain paper wearing the bundle's model
// and moves around like any other item
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
        ItemStack backpack = create();
        if (backpack == null) return;

        PlayerInventory inventory = player.getInventory();
        boolean found = false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!isBackpack(inventory.getItem(slot))) continue;
            // Also swaps out the real bundle handed out before
            inventory.setItem(slot, found ? null : backpack);
            found = true;
        }
        if (found) return;

        ItemStack held = inventory.getItem(PREFERRED_SLOT);
        if (held == null || held.getType().isAir()) inventory.setItem(PREFERRED_SLOT, backpack);
        else inventory.addItem(backpack);
    }

    private ItemStack create() {
        ItemStack template = registry.getAllItems().get(ITEM_ID);
        if (template == null) return null;

        ItemStack item = template.clone();
        item.editMeta(meta -> {
            meta.setItemModel(NamespacedKey.minecraft("bundle"));
            meta.setMaxStackSize(1);
        });
        return item;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isBackpack(event.getItem())) return;

        event.setCancelled(true);
        backpacks.open(event.getPlayer());
    }

    // Right clicking it in the inventory opens the backpack too, every other click
    // moves it like any item
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!event.isRightClick() || !isBackpack(event.getCurrentItem()) || !isEmpty(event.getCursor())) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);
        // Next tick, a menu can't be opened from inside a click
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) backpacks.open(player);
        });
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
