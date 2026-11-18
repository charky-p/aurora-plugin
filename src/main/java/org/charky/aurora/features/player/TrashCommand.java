package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.tool.ToolManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

// /trash, from the old Skript: a chest that deletes whatever is still in it when it is
// closed. Tools, the backpack and keys are handed back instead, they
// cannot be bought again
public class TrashCommand implements CommandExecutor, Listener {
    private static final int SIZE = 36;

    private final DataRegistry registry;

    public TrashCommand(DataRegistry registry) {
        this.registry = registry;
    }

    private static final class Bin implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return false;
        Bin bin = new Bin();
        bin.inventory = Bukkit.createInventory(bin, SIZE, TextUtils.parse("<dark_gray>Trash <gray>- closing deletes it all"));
        player.openInventory(bin.inventory);
        return true;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Bin)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        int deleted = 0;
        for (ItemStack item : event.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (kept(item)) {
                Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                left.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
            } else {
                deleted += item.getAmount();
            }
        }
        event.getInventory().clear();
        if (deleted > 0) player.sendActionBar(Component.text("Deleted " + deleted + " item" + (deleted == 1 ? "" : "s")));
    }

    // What the trash refuses: tools, the backpack and keys
    private boolean kept(ItemStack item) {
        if (ToolManager.toolId(item) != null) return true;
        String id = DataRegistry.itemId(item);
        return id != null && (id.equals(BackpackItemListener.ITEM_ID) || registry.isKey(id));
    }
}
