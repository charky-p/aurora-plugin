package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Restock, the Gauntlet bow's and crossbow's own enchant (gear_enchants.csv): a player
// carrying one gets a few arrows back every so often, wherever the setup is handed out
// (runs, duels, the PvP zone). The best Restock carried counts; its arrows and seconds a
// level are restock_arrows_N and restock_seconds_N in settings.csv, and nothing is given
// past restock_cap arrows. Like everything picked up there, they go when the setup comes back
public class GauntletRestock {
    // The Restock level on a piece of gear, set by GauntletGear
    public static final NamespacedKey RESTOCK_KEY = new NamespacedKey("aurora", "restock");
    public static final String ENCHANT = "restock";

    private final GauntletManager manager;
    // When each player's next arrows come, in seconds counted by everySecond
    private final Map<UUID, Long> next = new HashMap<>();
    private long seconds = 0;

    public GauntletRestock(GauntletManager manager) {
        this.manager = manager;
    }

    public void everySecond() {
        seconds++;
        next.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = manager.isHere(player) && player.getGameMode() != GameMode.SPECTATOR ? levelOf(player) : 0;
            if (level <= 0) {
                next.remove(player.getUniqueId());
                continue;
            }
            GauntletData data = manager.data();
            long every = Math.max(1, Math.round(data.setting("restock_seconds_" + level, 60)));
            long due = next.computeIfAbsent(player.getUniqueId(), k -> seconds + every);
            if (seconds < due) continue;

            next.put(player.getUniqueId(), seconds + every);
            int room = (int) data.setting("restock_cap", 128) - arrows(player);
            int amount = Math.min(room, (int) data.setting("restock_arrows_" + level, 4));
            if (amount <= 0) continue;
            player.getInventory().addItem(new ItemStack(Material.ARROW, amount));
            player.sendActionBar(TextUtils.parse("<green>Restock <gray>+" + amount + " arrows"));
            player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.6f, 1.2f);
        }
    }

    // The best Restock the player carries anywhere, 0 for none
    static int levelOf(Player player) {
        int best = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            Integer level = item.getItemMeta().getPersistentDataContainer().get(RESTOCK_KEY, PersistentDataType.INTEGER);
            if (level != null) best = Math.max(best, level);
        }
        return best;
    }

    private static int arrows(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) count += item.getAmount();
        }
        return count;
    }
}
