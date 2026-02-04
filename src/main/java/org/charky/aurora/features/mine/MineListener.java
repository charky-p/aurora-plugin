package org.charky.aurora.features.mine;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.common.loot.InventoryManager;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.common.loot.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MineListener implements Listener {
    private final MineManager manager;
    private final ScoreboardManager scoreboard;
    private final InventoryManager inventoryManager;
    private final DataRegistry registry;

    public MineListener(Aurora plugin) {
        this.manager = plugin.getMineManager();
        this.scoreboard = plugin.getScoreboardManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.registry = plugin.getRegistry();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        event.setDropItems(false);
        if (!player.isOp()) event.setCancelled(true);

        List<MenuItem> drops = new ArrayList<>();
        processBlock(player, event.getBlock(), drops);

        // Blast mining
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.AIR && item.hasItemMeta() && item.lore() != null)  {
            boolean isBlast = item.lore().stream()
                    .anyMatch(line -> PlainTextComponentSerializer.plainText()
                            .serialize(line).contains("Blast"));

            // 3x3x3 mining
            if (isBlast) {
                Block center = event.getBlock();
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            Block target = center.getRelative(x, y, z);
                            if (target.getType() != Material.BEDROCK && target.getType() != Material.AIR) {
                                processBlock(player, target, drops);
                            }
                        }
                    }
                }
            }
        }

        scoreboard.updateBoard(player);
        inventoryManager.addToInventory(player, drops);
    }

    private void processBlock(Player player, Block block, List<MenuItem> drops) {
        manager.handleBlockBreak(player, block.getLocation());

        String dropId = manager.getBlockIdAt(block.getLocation());

        if (registry.hasBlock(dropId)) {
            List<MenuItem> blockDrops = registry.getBlockDrops(dropId, getFortuneLevel(player));
            drops.addAll(blockDrops);

            // Essence
            double baseChance = 0.001;
            int luckLevel = getCustomEnchantLevel(player, "luck");

            // Essence Chance = 0.1% + (0.1% * Level)
            double finalChance = baseChance + (baseChance * luckLevel);

            if (Math.random() < finalChance) {
                scoreboard.addEssence(player, ThreadLocalRandom.current().nextInt(1, 4));
                TextUtils.send(player, "<gradient:light_purple:dark_purple><b>RARE!</b></gradient> <gray>You found <white>Essence<gray>!");
            }

            // Only break if it is a block with a drop
            if (!drops.isEmpty()) {
                scoreboard.incrementMined(player);
                block.setType(Material.AIR, false);
            }
        }
    }

    private int getFortuneLevel(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            return 0;
        }

        return item.getEnchantmentLevel(Enchantment.FORTUNE);
    }

    private int getCustomEnchantLevel(Player player, String enchantId) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir() || !item.hasItemMeta()) return 0;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER)) return 0;

        PersistentDataContainer enchantPdc = pdc.get(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER);

        NamespacedKey key = new NamespacedKey("aurora", enchantId.toLowerCase());
        return enchantPdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }
}
