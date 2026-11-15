package org.charky.aurora.features.fishing;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.loot.MenuItem;
import org.charky.aurora.common.util.Enchants;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.features.collection.CollectionManager;
import org.charky.aurora.features.player.BackpackManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Fishing with one of the rods from gear_other.csv. The vanilla catch is thrown
// away and the rod's table decides what comes up, see FishingTable. Fish are
// resources, so they go to the backpack and count towards collections
public class FishingListener implements Listener {
    private final DataRegistry registry;
    private final BackpackManager backpacks;
    private final CollectionManager collections;

    public FishingListener(Aurora plugin) {
        this.registry = plugin.getRegistry();
        this.backpacks = plugin.getBackpackManager();
        this.collections = plugin.getCollectionManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        // No vanilla fish, books or XP
        if (event.getCaught() != null) event.getCaught().remove();
        event.setExpToDrop(0);

        Player player = event.getPlayer();
        ItemStack rod = rod(player);
        if (rod == null) {
            player.sendActionBar(TextUtils.parse("<red>This rod catches nothing here"));
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        FishingTable.Catch caught = FishingTable.roll(DataRegistry.itemId(rod), random.nextInt(1, 101),
                Enchants.level(rod, "luck"), Enchants.level(rod, "fortune"), random.nextInt(1, 4));
        if (caught == null) return;

        List<MenuItem> drops = new ArrayList<>();
        add(drops, caught.itemId(), caught.amount());
        if (random.nextDouble() < FishingTable.TREASURE_CHANCE) add(drops, FishingTable.TREASURE, 1);
        if (drops.isEmpty()) return;

        if (caught.rare() || drops.size() > 1) {
            TextUtils.send(player, "<gold><b>You caught something rare!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        }
        // Last is what the action bar shows, the fish before any treasure
        backpacks.get(player.getUniqueId()).collect(player, drops.reversed());
        collections.add(player, drops);
    }

    // The rod being cast, in either hand
    private static ItemStack rod(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (FishingTable.isRod(DataRegistry.itemId(main))) return main;

        ItemStack off = player.getInventory().getItemInOffHand();
        return FishingTable.isRod(DataRegistry.itemId(off)) ? off : null;
    }

    // A fish missing from the data is skipped, never a crash
    private void add(List<MenuItem> drops, String id, int amount) {
        ItemStack item = registry.getAllItems().get(id);
        if (item == null) return;

        MenuItem drop = new MenuItem(id, item);
        drop.setAmount(amount);
        drops.add(drop);
    }
}
