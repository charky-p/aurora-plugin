package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.charky.aurora.common.util.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Gauntlet parties for co-op runs: /gauntlet party invite <player> (open for INVITE_MS),
// accept, decline, leave, kick and list. Up to party_max players; the leader starts
// runs for everyone. Leaving the server leaves the party; a leader who leaves hands
// it to the next member
public class GauntletParty implements Listener {
    private static final long INVITE_MS = 60_000;

    private record Invite(UUID leader, long expires) {
    }

    private static final class Party {
        UUID leader;
        final List<UUID> members = new ArrayList<>();
        // Anyone in the Gauntlet can join, no invite needed
        boolean open;

        Party(UUID leader) {
            this.leader = leader;
            members.add(leader);
        }
    }

    private final GauntletManager manager;
    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public GauntletParty(GauntletManager manager) {
        this.manager = manager;
    }

    public boolean inParty(Player player) {
        return byMember.containsKey(player.getUniqueId());
    }

    public boolean isLeader(Player player) {
        Party party = byMember.get(player.getUniqueId());
        return party == null || party.leader.equals(player.getUniqueId());
    }

    // The player's party online, the leader first; just the player outside a party
    public List<Player> team(Player player) {
        Party party = byMember.get(player.getUniqueId());
        if (party == null) return List.of(player);
        List<Player> team = new ArrayList<>();
        Player leader = Bukkit.getPlayer(party.leader);
        if (leader != null) team.add(leader);
        for (UUID id : party.members) {
            Player member = Bukkit.getPlayer(id);
            if (member != null && !id.equals(party.leader)) team.add(member);
        }
        return team;
    }

    public int max() {
        return Math.max(2, (int) manager.data().setting("party_max", 4));
    }

    // Who invited the player, null without an invite still open
    public Player inviter(Player player) {
        Invite invite = invites.get(player.getUniqueId());
        return invite == null || invite.expires() < System.currentTimeMillis() ? null : Bukkit.getPlayer(invite.leader());
    }

    // Null if the invite was sent, otherwise why not
    public String invite(Player leader, Player target) {
        if (target == null) return "That player is not online";
        if (target.equals(leader)) return "You can't invite yourself";
        if (!manager.isHere(leader)) return "You are not in the Gauntlet";
        if (!manager.isHere(target)) return target.getName() + " is not in the Gauntlet";
        if (inParty(target)) return target.getName() + " is already in a party";
        Party party = byMember.get(leader.getUniqueId());
        if (party != null && !party.leader.equals(leader.getUniqueId())) return "Only the party leader can invite";
        if (party != null && party.members.size() >= max()) return "The party is full (" + max() + ")";
        invites.put(target.getUniqueId(), new Invite(leader.getUniqueId(), System.currentTimeMillis() + INVITE_MS));
        TextUtils.send(leader, "<gray>Party invite sent to <white>" + target.getName());
        TextUtils.send(target, "<aqua><b>PARTY</b> <white>" + leader.getName() + " <gray>invites you to climb the Gauntlet together! "
                + "<click:run_command:'/gauntlet party accept'><hover:show_text:'<gray>Join their party'><green><b>[ACCEPT]</b></hover></click> "
                + "<click:run_command:'/gauntlet party decline'><hover:show_text:'<gray>Not now'><red><b>[DECLINE]</b></hover></click>");
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.4f);
        return null;
    }

    public String accept(Player target) {
        Invite invite = invites.remove(target.getUniqueId());
        if (invite == null || invite.expires() < System.currentTimeMillis()) return "You have no party invite";
        if (inParty(target)) return "You are already in a party";
        if (!manager.isHere(target)) return "You are not in the Gauntlet";
        Player leader = Bukkit.getPlayer(invite.leader());
        if (leader == null) return "They are not online any more";
        Party party = byMember.computeIfAbsent(leader.getUniqueId(), Party::new);
        if (!party.leader.equals(leader.getUniqueId())) return "They are not leading a party any more";
        if (party.members.size() >= max()) return "The party is full";
        add(party, target);
        return null;
    }

    // Straight into an open party, no invite needed. Null if it worked, otherwise why not
    public String join(Player player, Player leader) {
        if (leader == null) return "That player is not online";
        if (inParty(player)) return "You are already in a party";
        if (!manager.isHere(player)) return "You are not in the Gauntlet";
        Party party = byMember.get(leader.getUniqueId());
        if (party == null || !party.open || !party.leader.equals(leader.getUniqueId())) return leader.getName() + " has no open party";
        if (party.members.size() >= max()) return "The party is full";
        invites.remove(player.getUniqueId());
        add(party, player);
        return null;
    }

    private void add(Party party, Player player) {
        party.members.add(player.getUniqueId());
        byMember.put(player.getUniqueId(), party);
        tell(party, "<aqua><b>PARTY</b> <white>" + player.getName() + " <gray>joined (" + party.members.size() + "/" + max() + ")");
        refresh(party);
    }

    // Opens the player's party to anyone in the Gauntlet, telling everyone there, or closes
    // it to invites only. Someone in no party opens one of their own. Null if it worked,
    // otherwise why not
    public String toggleOpen(Player player) {
        if (!manager.isHere(player)) return "You are not in the Gauntlet";
        Party party = byMember.get(player.getUniqueId());
        if (party != null && !party.leader.equals(player.getUniqueId())) return "Only the party leader can open or close it";
        if (party == null) {
            party = new Party(player.getUniqueId());
            byMember.put(player.getUniqueId(), party);
        }
        party.open = !party.open;
        if (party.open) {
            String call = "<aqua><b>PARTY</b> <white>" + player.getName() + " <gray>is looking for players (" + party.members.size() + "/"
                    + max() + "): <click:run_command:'/gauntlet party join " + player.getName() + "'><hover:show_text:'<gray>Join "
                    + player.getName() + "'s party'><green><b>[JOIN]</b></hover></click>";
            for (Player other : player.getWorld().getPlayers()) if (!party.members.contains(other.getUniqueId())) TextUtils.send(other, call);
            tell(party, "<gray>Your party is open, anyone in the Gauntlet can join");
        } else {
            tell(party, "<gray>Your party is closed, invites only");
            // A party of one closed again is no party
            if (party.members.size() == 1) byMember.remove(player.getUniqueId());
        }
        refresh(party);
        return null;
    }

    public boolean isOpen(Player player) {
        Party party = byMember.get(player.getUniqueId());
        return party != null && party.open;
    }

    // The leaders of open parties with room, online
    public List<Player> openLeaders() {
        List<Player> leaders = new ArrayList<>();
        for (Party party : new HashSet<>(byMember.values())) {
            Player leader = Bukkit.getPlayer(party.leader);
            if (party.open && party.members.size() < max() && leader != null) leaders.add(leader);
        }
        return leaders;
    }

    // How many a player's party has, 1 outside one
    public int size(Player player) {
        Party party = byMember.get(player.getUniqueId());
        return party == null ? 1 : party.members.size();
    }

    public String decline(Player target) {
        Invite invite = invites.remove(target.getUniqueId());
        if (invite == null) return "You have no party invite";
        Player leader = Bukkit.getPlayer(invite.leader());
        if (leader != null) TextUtils.send(leader, "<gray>" + target.getName() + " declined your party invite");
        TextUtils.send(target, "<gray>Invite declined");
        return null;
    }

    public String leave(Player player) {
        Party party = byMember.remove(player.getUniqueId());
        if (party == null) return "You are not in a party";
        party.members.remove(player.getUniqueId());
        TextUtils.send(player, "<gray>You left the party");
        manager.refreshBoard(player);
        if (party.members.size() <= 1) {
            for (UUID id : party.members) {
                byMember.remove(id);
                Player last = Bukkit.getPlayer(id);
                if (last != null) {
                    TextUtils.send(last, "<gray>Your party is over, " + player.getName() + " left");
                    manager.refreshBoard(last);
                }
            }
            return null;
        }
        if (party.leader.equals(player.getUniqueId())) party.leader = party.members.getFirst();
        tell(party, "<aqua><b>PARTY</b> <white>" + player.getName() + " <gray>left. Leader: <white>" + GauntletManager.nameOf(party.leader));
        refresh(party);
        return null;
    }

    public String kick(Player leader, Player target) {
        Party party = byMember.get(leader.getUniqueId());
        if (party == null || !party.leader.equals(leader.getUniqueId())) return "Only the party leader can kick";
        if (target == null || !party.members.contains(target.getUniqueId()) || target.equals(leader)) return "They are not in your party";
        TextUtils.send(target, "<gray>You were removed from the party");
        return leave(target);
    }

    public void list(Player player) {
        Party party = byMember.get(player.getUniqueId());
        if (party == null) {
            TextUtils.send(player, "<gray>You are not in a party. <white>/gauntlet party invite <player></white> to start one");
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID id : party.members) names.add((id.equals(party.leader) ? "<gold>★ " : "<white>") + GauntletManager.nameOf(id));
        TextUtils.send(player, "<aqua><b>PARTY</b> <gray>(" + party.members.size() + "/" + max() + ")" + (party.open ? " <green>open" : "")
                + " " + String.join("<gray>, ", names));
    }

    private void refresh(Party party) {
        for (UUID id : party.members) {
            Player member = Bukkit.getPlayer(id);
            if (member != null) manager.refreshBoard(member);
        }
    }

    private static void tell(Party party, String message) {
        for (UUID id : party.members) {
            Player member = Bukkit.getPlayer(id);
            if (member != null) TextUtils.send(member, message);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invites.remove(event.getPlayer().getUniqueId());
        if (inParty(event.getPlayer())) leave(event.getPlayer());
    }
}
