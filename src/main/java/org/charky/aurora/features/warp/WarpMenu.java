package org.charky.aurora.features.warp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.keys.KeyItems;
import org.charky.aurora.features.trader.TraderClose;

import java.util.ArrayList;
import java.util.List;

// One warp per region, opened from the menu. A region is locked until the player
// has the key its portal asks for, so nobody skips a gate by warping
public class WarpMenu extends PagedMenu {
    private final Aurora plugin;
    private final List<Warp> warps;

    public WarpMenu(Aurora plugin) {
        super(plugin.getRegistry(), "Warps");
        this.plugin = plugin;
        this.warps = plugin.getRegistry().getWarps();
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (Warp warp : warps) {
            boolean open = unlocked(player, warp);
            ItemStack item = open ? warp.icon().clone() : new ItemStack(Material.GRAY_DYE);
            Component name = warp.icon().getItemMeta().displayName();
            item.editMeta(meta -> {
                meta.displayName(name);

                List<Component> lore = new ArrayList<>();
                if (!open) lore.add(KeyItems.missing(registry, warp.tag()).decoration(TextDecoration.ITALIC, false));
                else if (destination(warp) == null) lore.add(TextUtils.parse("<!italic><red>Not open yet"));
                else lore.add(TextUtils.parse("<!italic><yellow>Click to warp"));
                meta.lore(lore);
            });
            items.add(item);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        long open = warps.stream().filter(warp -> unlocked(player, warp)).count();
        return List.of(TextUtils.parse("<!italic><gray>Unlocked: <white>" + open + "/" + warps.size()));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= warps.size()) return;

        Warp warp = warps.get(index);
        if (!unlocked(player, warp)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }

        Location destination = destination(warp);
        if (destination == null) {
            TextUtils.error(player, "That region is not open yet.");
            return;
        }

        // Next tick, teleporting to another world with a menu open is unreliable
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            player.closeInventory();
            player.teleport(destination);
            player.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1f);
        });
    }

    private static boolean unlocked(Player player, Warp warp) {
        return warp.tag() == null || KeyItems.has(player, warp.tag());
    }

    // The spawn set with /aurora setspawn in the warp's world, null without one
    private Location destination(Warp warp) {
        World world = Bukkit.getWorld(warp.world());
        return world == null ? null : plugin.getSpawn(world);
    }
}
