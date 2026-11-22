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
import java.util.function.Supplier;

// The party: an invite waiting to be accepted, the members (the leader removes one with
// a click), leaving it, and a head for everyone in the Gauntlet the leader can invite.
// Everything /gauntlet party does, for players who can't click chat
public class PartyMenu extends PagedMenu {
    private final GauntletManager manager;
    // What each entry does when clicked, null if nothing: null if it worked, otherwise why not
    private List<Supplier<String>> actions = List.of();

    public PartyMenu(GauntletManager manager) {
        super(manager.registry(), "Party");
        this.manager = manager;
    }

    @Override
    protected List<ItemStack> entries(Player player) {
        GauntletParty party = manager.party();
        List<ItemStack> items = new ArrayList<>();
        List<Supplier<String>> clicks = new ArrayList<>();
        Player inviter = party.inviter(player);
        if (inviter != null && !party.inParty(player)) {
            items.add(button(Material.LIME_DYE, "<green><b>Join " + inviter.getName() + "'s party", List.of("<yellow>Click to accept")));
            clicks.add(() -> party.accept(player));
            items.add(button(Material.GRAY_DYE, "<red><b>Decline the invite", List.of("<gray>Click to decline")));
            clicks.add(() -> party.decline(player));
        }
        if (!party.inParty(player)) {
            for (Player leader : party.openLeaders()) {
                if (leader.equals(player)) continue;
                items.add(head(leader, "<white>" + leader.getName() + "'s party <gray>(" + party.size(leader) + "/" + party.max() + ")",
                        List.of("<green>Open to anyone", "<yellow>Click to join")));
                clicks.add(() -> party.join(player, leader));
            }
        }
        boolean leader = party.isLeader(player);
        List<Player> team = party.team(player);
        if (leader) {
            boolean open = party.isOpen(player);
            items.add(button(open ? Material.OAK_FENCE_GATE : Material.LIME_DYE, open ? "<red><b>Close the party" : "<green><b>Open the party",
                    List.of("<gray>Anyone in the Gauntlet can join", "<gray>an open party, no invite needed.", "",
                            open ? "<yellow>Click to close it" : "<yellow>Click to open it and tell everyone")));
            clicks.add(() -> party.toggleOpen(player));
        }
        if (party.inParty(player)) {
            for (Player member : team) {
                boolean leads = member.equals(team.getFirst());
                items.add(head(member, (leads ? "<gold>★ " : "<white>") + member.getName(),
                        leads ? List.of("<gray>Leader, starts the runs") : leader ? List.of("<red>Click to remove") : List.of()));
                clicks.add(leader && !leads ? () -> party.kick(player, member) : null);
            }
            items.add(button(Material.BARRIER, "<red><b>Leave the party", List.of("<gray>Click to leave")));
            clicks.add(() -> party.leave(player));
        }
        if (leader && team.size() < party.max()) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(player) || !manager.isHere(other) || party.inParty(other)) continue;
                items.add(head(other, "<white>" + other.getName(), List.of("<yellow>Click to invite")));
                clicks.add(() -> party.invite(player, other));
            }
        }
        actions = clicks;
        return items;
    }

    private static ItemStack head(Player owner, String name, List<String> lore) {
        ItemStack item = button(Material.PLAYER_HEAD, name, lore);
        item.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(owner));
        return item;
    }

    @Override
    protected List<Component> info(Player player, List<ItemStack> entries) {
        return List.of(TextUtils.parse("<!italic><gray>Climb together: more mobs, tougher"),
                TextUtils.parse("<!italic><gray>bosses, one pot of Shards for all."),
                TextUtils.parse("<!italic><gray>Up to <white>" + manager.party().max() + "</white> players, the leader starts."));
    }

    @Override
    protected void onEntryClick(Player player, int index) {
        if (index >= actions.size() || actions.get(index) == null) return;
        String problem = actions.get(index).get();
        if (problem != null) TextUtils.error(player, problem);
        render(player);
    }

    @Override
    protected void back(Player player) {
        manager.openMenu(player);
    }
}
