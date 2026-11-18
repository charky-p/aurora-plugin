package org.charky.aurora.features.player;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.DataRegistry;

import java.util.ArrayList;
import java.util.List;

// /leaderboard: the top ten of one stat as player heads, gold, silver and bronze for the
// first three, and where you stand. The arrows at the bottom switch between stats
public class LeaderboardMenu extends PagedMenu {
    private static final int SIZE = 10;
    private static final int PREVIOUS_STAT = 46;
    private static final int NEXT_STAT = 52;
    private static final String[] PLACE_COLOUR = {"<gold>", "<#C0C0C0>", "<#CD7F32>"};

    private final PlayerManager players;
    private LeaderboardStat stat;
    private List<PlayerManager.Entry> top = List.of();
    private int place = -1;

    private LeaderboardMenu(DataRegistry registry, PlayerManager players, LeaderboardStat stat) {
        super(registry, "Leaderboard");
        this.players = players;
        this.stat = stat;
    }

    public static void open(DataRegistry registry, PlayerManager players, Player player) {
        LeaderboardMenu menu = new LeaderboardMenu(registry, players, LeaderboardStat.LEVEL);
        menu.load(player, true);
    }

    private void load(Player player, boolean first) {
        players.top(stat, SIZE, player.getUniqueId(), (entries, standing) -> {
            top = entries;
            place = standing;
            if (!player.isOnline()) return;
            if (first) open(player);
            else render(player);
        });
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            PlayerManager.Entry entry = top.get(i);
            OfflinePlayer who = Bukkit.getOfflinePlayer(entry.uuid());
            String name = who.getName() == null ? "Unknown" : who.getName();
            String colour = i < PLACE_COLOUR.length ? PLACE_COLOUR[i] : "<white>";
            int rank = i + 1;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(SkullMeta.class, meta -> {
                meta.setOwningPlayer(who);
                meta.displayName(TextUtils.parse("<!italic>" + colour + "<b>#" + rank + "</b> <white>" + name));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic><gray>" + stat.title() + ": <yellow>" + stat.format(entry.value())));
                if (player.getUniqueId().equals(entry.uuid())) lore.add(TextUtils.parse("<!italic><green>That's you"));
                meta.lore(lore);
            });
            items.add(head);
        }
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        PlayerData mine = players.get(player);
        List<Component> lines = new ArrayList<>();
        lines.add(TextUtils.parse("<!italic><gray>Ranked by <yellow>" + stat.title()));
        if (mine != null) {
            lines.add(TextUtils.parse("<!italic><gray>You: <white>" + stat.format(stat.of(mine))
                    + (place > 0 ? " <dark_gray>(<white>#" + place + "<dark_gray>)" : "")));
        }
        if (top.isEmpty()) lines.add(TextUtils.parse("<!italic><dark_gray>Nobody has played yet"));
        return lines;
    }

    @Override
    protected void decorate(Player player, Inventory inventory) {
        inventory.setItem(PREVIOUS_STAT, statButton(stat.next(-1), "<yellow>« "));
        inventory.setItem(NEXT_STAT, statButton(stat.next(1), "<yellow>» "));
    }

    private static ItemStack statButton(LeaderboardStat target, String arrow) {
        ItemStack item = new ItemStack(target.icon());
        item.editMeta(meta -> {
            meta.displayName(TextUtils.parse("<!italic>" + arrow + target.title()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    @Override
    protected void onButtonClick(Player player, int slot) {
        if (slot == PREVIOUS_STAT) stat = stat.next(-1);
        else if (slot == NEXT_STAT) stat = stat.next(1);
        else return;
        load(player, false);
    }
}
