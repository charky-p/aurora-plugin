package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Everyone in a run or a duel right now, a head each, click to watch
public class SpectateMenu extends PagedMenu {
    private final GauntletManager manager;
    private List<UUID> shown = List.of();

    public SpectateMenu(GauntletManager manager) {
        super(manager.registry(), "Spectate");
        this.manager = manager;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        List<UUID> live = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player) || !(manager.inRun(other) || manager.inDuel(other))) continue;
            live.add(other.getUniqueId());
            boolean run = manager.inRun(other);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(SkullMeta.class, meta -> {
                meta.setOwningPlayer(other);
                meta.displayName(TextUtils.parse("<!italic><white><b>" + other.getName()));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtils.parse("<!italic>" + (run ? "<red>Floor " + manager.runs().floorOf(other)
                        : "<gold>Duel vs " + (manager.duels().opponent(other) == null ? "?" : manager.duels().opponent(other).getName()))));
                lore.add(TextUtils.parse("<!italic><yellow>Click to watch"));
                meta.lore(lore);
            });
            items.add(head);
        }
        shown = live;
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse(entries.isEmpty() ? "<!italic><gray>Nobody is in a run or a duel right now"
                : "<!italic><gray>Watch a run or a duel live"));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= shown.size()) return;
        String problem = manager.spectate().watch(player, Bukkit.getPlayer(shown.get(index)));
        if (problem != null) {
            TextUtils.error(player, problem);
            render(player);
        }
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
