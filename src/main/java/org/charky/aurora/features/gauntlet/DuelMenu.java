package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.charky.aurora.common.menu.PagedMenu;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Duels without typing: a challenge waiting for the player to accept or decline, and a
// head for everyone in the Gauntlet free to fight, with their rating, to challenge
public class DuelMenu extends PagedMenu {
    private final GauntletManager manager;
    // What each entry of the last render does: null if it worked, otherwise why not
    private List<Supplier<String>> actions = List.of();

    public DuelMenu(GauntletManager manager) {
        super(manager.registry(), "Duels");
        this.manager = manager;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletDuels duels = manager.duels();
        List<ItemStack> items = new ArrayList<>();
        List<Supplier<String>> clicks = new ArrayList<>();
        Player challenger = duels.challenger(player);
        if (challenger != null) {
            items.add(button(Material.LIME_DYE, "<green><b>Fight " + challenger.getName(), List.of(
                    "<gray>They challenged you, rating <gold>" + GauntletDuels.rating(manager.profile(challenger)), "<yellow>Click to accept")));
            clicks.add(() -> duels.accept(player));
            items.add(button(Material.GRAY_DYE, "<red><b>Decline", List.of("<gray>Click to decline")));
            clicks.add(() -> duels.decline(player));
        }
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player) || other.equals(challenger) || duels.canDuel(other) != null) continue;
            items.add(head(other, "<white>" + other.getName(), List.of(
                    "<gray>Rating <gold>" + GauntletDuels.rating(manager.profile(other)), "<yellow>Click to challenge")));
            clicks.add(() -> duels.challenge(player, other));
        }
        actions = clicks;
        return items;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>A 1v1 in your own arena: wins"),
                TextUtils.parse("<!italic><gray>raise your rating and pay Shards."),
                TextUtils.parse(entries.isEmpty() ? "<!italic><dark_gray>Nobody is free to fight right now"
                        : "<!italic><gray>Your rating: <gold>" + GauntletDuels.rating(manager.profile(player))));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= actions.size()) return;
        String problem = actions.get(index).get();
        if (problem != null) {
            TextUtils.error(player, problem);
            render(player);
        } else {
            player.closeInventory();
        }
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
