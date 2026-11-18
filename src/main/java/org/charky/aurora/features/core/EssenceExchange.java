package org.charky.aurora.features.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.player.BackpackData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The Essence Trader in Golem's Rest, the endless source of Essence once the game is
// done: only a player who has maxed every zone may trade. Every resource in the
// backpack is bought at its essence value (resources.csv, worked out by
// scripts/balance/essence_values.py from how fast an endgame player farms it, more for
// denser blocks and mobs). Click one to sell all of it, or sell the whole backpack
// with a second click to be sure
public final class EssenceExchange extends PagedMenu {
    private static final int SELL_ALL_SLOT = 52;
    private static final long CONFIRM_MS = 5000;

    private final Aurora plugin;
    private List<String> shown = List.of();
    private long armedAt;

    private EssenceExchange(Aurora plugin) {
        super(plugin.getRegistry(), "Essence Trader");
        this.plugin = plugin;
    }

    public static void open(Aurora plugin, Player player) {
        if (!plugin.getToolManager().maxedAll(player)) {
            TextUtils.error(player, "The Essence Trader only deals with those who have maxed every zone");
            return;
        }
        new EssenceExchange(plugin).open(player);
    }

    private BackpackData backpack(Player player) {
        return plugin.getBackpackManager().get(player.getUniqueId());
    }

    // What a resource's whole stack in the backpack is worth, whole Essence only
    private long worth(String id, long amount) {
        return (long) Math.floor(amount * registry.getEssenceValue(id));
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<String> ids = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        BackpackData backpack = backpack(player);
        if (backpack == null) return items;
        for (Map.Entry<String, Long> entry : backpack.getItems().entrySet()) {
            String id = entry.getKey();
            long amount = entry.getValue();
            long worth = worth(id, amount);
            if (amount <= 0 || worth <= 0) continue;
            ItemStack icon = registry.getAllItems().get(id);
            if (icon == null) continue;
            ItemStack item = icon.clone();
            item.setAmount(1);
            item.editMeta(meta -> {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>In your backpack: <white>" + String.format("%,d", amount)));
                lore.add(TextUtils.parse("<!italic><gray>Worth: <gold>" + String.format("%,d", worth) + " Essence"));
                lore.add(Component.empty());
                lore.add(TextUtils.parse("<!italic><yellow>Click to sell all of it"));
                meta.lore(lore);
            });
            ids.add(id);
            items.add(item);
        }
        shown = ids;
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        long total = 0;
        BackpackData backpack = backpack(player);
        if (backpack != null) {
            for (Map.Entry<String, Long> entry : backpack.getItems().entrySet()) total += worth(entry.getKey(), entry.getValue());
        }
        return List.of(
                TextUtils.parse("<!italic><gray>Your backpack is worth <gold>" + String.format("%,d", total) + " Essence"),
                TextUtils.parse("<!italic><dark_gray>Denser zones pay more for the time"));
    }

    @Override
    protected void decorate(Player player, Inventory inventory) {
        boolean armed = System.currentTimeMillis() - armedAt < CONFIRM_MS;
        ItemStack sellAll = button(armed ? Material.RED_CONCRETE : Material.GOLD_BLOCK,
                armed ? "<red><b>Click again to sell everything" : "<gold><b>Sell everything");
        inventory.setItem(SELL_ALL_SLOT, sellAll);
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot != SELL_ALL_SLOT) return;
        if (System.currentTimeMillis() - armedAt >= CONFIRM_MS) {
            armedAt = System.currentTimeMillis();
            render(player);
            return;
        }
        armedAt = 0;
        long total = 0;
        BackpackData backpack = backpack(player);
        if (backpack != null) {
            for (Map.Entry<String, Long> entry : Map.copyOf(backpack.getItems()).entrySet()) total += sell(player, entry.getKey());
        }
        paid(player, total);
        render(player);
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index < 0 || index >= shown.size()) return;
        paid(player, sell(player, shown.get(index)));
        render(player);
    }

    // Sells all of one resource, Essence for it
    private long sell(Player player, String id) {
        BackpackData backpack = backpack(player);
        if (backpack == null) return 0;
        long amount = backpack.getItems().getOrDefault(id, 0L);
        long worth = worth(id, amount);
        if (worth <= 0) return 0;
        backpack.removeItem(id, amount);
        return worth;
    }

    private void paid(Player player, long essence) {
        if (essence <= 0) return;
        plugin.getPlayerManager().addEssence(player, (int) Math.min(Integer.MAX_VALUE, essence));
        plugin.getScoreboardManager().updateBoard(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
        TextUtils.success(player, "Sold for " + String.format("%,d", essence) + " Essence");
    }
}
