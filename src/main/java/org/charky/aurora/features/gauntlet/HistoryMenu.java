package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

// A player's last runs, newest first: how far each went and how it ended, its time, heat
// and party, the kills, what it banked and the boons taken
public class HistoryMenu extends PagedMenu {
    private static final int BOONS_A_LINE = 3;

    private final GauntletManager manager;

    public HistoryMenu(GauntletManager manager) {
        super(manager.registry(), "Your Runs");
        this.manager = manager;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        long now = System.currentTimeMillis();
        List<ItemStack> items = new ArrayList<>();
        for (RunRecord run : manager.profile(player).history) items.add(entry(run, now));
        return items;
    }

    private ItemStack entry(RunRecord run, long now) {
        List<String> lines = new ArrayList<>();
        lines.add("<gray>Cleared <white>" + run.cleared() + "</white> in <white>" + GauntletRuns.clock(run.seconds())
                + (run.startFloor() > 1 ? " <dark_gray>(from floor " + run.startFloor() + ")" : ""));
        if (run.heat() > 0) lines.add("<gold>Heat " + run.heat());
        if (run.party() > 1) lines.add("<aqua>A party of " + run.party());
        lines.add("<gray>Kills: <white>" + run.kills());
        lines.add("<gray>Banked: <aqua>" + run.shards() + " Shards" + (run.cores() > 0 ? " <light_purple>" + run.cores() + " Cores" : ""));
        List<String> boons = boons(run.boons());
        for (int i = 0; i < boons.size(); i += BOONS_A_LINE) {
            lines.add((i == 0 ? "<gray>Boons: " : "   ") + String.join("<gray>, ", boons.subList(i, Math.min(boons.size(), i + BOONS_A_LINE))));
        }
        lines.add("");
        lines.add("<dark_gray>" + ago(run.ended(), now));
        return button(run.died() ? Material.SKELETON_SKULL : Material.OAK_DOOR,
                (run.died() ? "<red>" : "<gray>") + "<b>Floor " + run.floor() + "</b> <gray>" + (run.died() ? "fell" : "left"), lines);
    }

    // "might:2,cleave:1" as the boons' names in their colours
    private List<String> boons(String saved) {
        List<String> names = new ArrayList<>();
        if (saved == null || saved.isBlank()) return names;
        for (String entry : saved.split(",")) {
            String[] parts = entry.split(":", 2);
            GauntletData.Boon boon = manager.data().boons().get(parts[0]);
            if (boon == null) continue;
            int stacks = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            names.add(boon.colour() + boon.name() + (stacks > 1 ? " x" + stacks : ""));
        }
        return names;
    }

    // How long ago, roughly: "just now", "5 minutes ago", "3 hours ago", "2 days ago"
    static String ago(long then, long now) {
        long minutes = Math.max(0, (now - then) / 60_000);
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse(entries.isEmpty() ? "<!italic><gray>No runs yet: walk into the portal"
                : "<!italic><gray>Your last " + entries.size() + " runs, newest first"));
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
