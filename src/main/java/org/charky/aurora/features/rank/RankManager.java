package org.charky.aurora.features.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.RankStore;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;
import org.charky.aurora.features.player.TabManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Which ranks a player owns and which one they wear. Everyone has MEMBER, playtime
// ranks come with hours online, shop ranks are bought with Essence. The worn rank
// shows in chat, the tab list and the sidebar, and changes nothing else. Donor and
// staff ranks are handed out by an admin
public class RankManager implements Listener {
    // Playtime is counted a minute at a time
    private static final long PLAYTIME_SECONDS = 60;
    private static final long PLAYTIME_TICKS = PLAYTIME_SECONDS * 20;

    private final Aurora plugin;
    private final Database db;
    private final RankStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final TabManager tab;
    // Shop ranks each player has bought
    private final Map<UUID, Set<String>> bought;
    private final BukkitTask playtime;
    // Told every time a minute of playtime is counted, with the seconds before and after
    private PlaytimePassed onPlaytime = (player, before, after) -> {};

    public interface PlaytimePassed {
        void passed(Player player, long before, long after);
    }

    public void setOnPlaytime(PlaytimePassed onPlaytime) {
        this.onPlaytime = onPlaytime;
    }

    public RankManager(Aurora plugin, Database db, RankStore store, PlayerManager players,
                       ScoreboardManager scoreboard, TabManager tab) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.tab = tab;
        this.bought = store.loadAll();
        tab.setRanks(this::prefix);
        this.playtime = Bukkit.getScheduler().runTaskTimer(plugin, this::countPlaytime, PLAYTIME_TICKS, PLAYTIME_TICKS);
    }

    // The rank a player wears, MEMBER while their profile is still loading
    public Rank worn(Player player) {
        PlayerData data = players.get(player);
        return data == null ? Ranks.MEMBER : Ranks.get(data.getRank());
    }

    // What goes before a name in chat and the tab list
    public Component prefix(Player player) {
        return worn(player).component();
    }

    private void unlocked(Player player, Rank rank) {
        player.sendMessage(TextUtils.parse("<gold><b>UNLOCKED</b> ").append(rank.component())
                .append(Component.text(" - put it on with /ranks", NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
    }

    public boolean owns(Player player, Rank rank) {
        return switch (rank.kind()) {
            case DEFAULT -> true;
            case PLAYTIME -> playtimeOf(player) >= rank.requirement();
            case SHOP, DONOR, STAFF -> bought.getOrDefault(player.getUniqueId(), Set.of()).contains(rank.id());
        };
    }

    public long playtimeOf(Player player) {
        PlayerData data = players.get(player);
        return data == null ? 0 : data.getPlaytime();
    }

    public int essenceOf(Player player) {
        PlayerData data = players.get(player);
        return data == null ? 0 : data.getEssence();
    }

    // Null if it is now worn, otherwise why not
    public String wear(Player player, Rank rank) {
        PlayerData data = players.get(player);
        if (data == null) return "Your profile is still loading";
        if (!owns(player, rank)) return "You have not unlocked that rank";

        data.setRank(rank.id());
        refresh(player);
        return null;
    }

    // Null if it was bought and is now worn, otherwise why not
    public String buy(Player player, Rank rank) {
        PlayerData data = players.get(player);
        if (data == null) return "Your profile is still loading";
        if (rank.kind() != Rank.Kind.SHOP) return "That rank is not for sale";
        if (owns(player, rank)) return "You already own that rank";
        if (!data.spendEssence((int) rank.requirement())) {
            return "You need " + (rank.requirement() - data.getEssence()) + " more Essence";
        }

        UUID uuid = player.getUniqueId();
        bought.computeIfAbsent(uuid, k -> new HashSet<>()).add(rank.id());
        db.async(() -> store.add(uuid, rank.id()));

        data.setRank(rank.id());
        refresh(player);
        player.sendMessage(TextUtils.parse("<gold><b>UNLOCKED</b> ").append(rank.component())
                .append(Component.text(" - change it any time with /ranks", NamedTextColor.GRAY)));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
        return null;
    }

    // For admins, the only way to a donor or staff rank. True if it changed anything
    public boolean grant(Player player, Rank rank) {
        UUID uuid = player.getUniqueId();
        if (!bought.computeIfAbsent(uuid, k -> new HashSet<>()).add(rank.id())) return false;
        db.async(() -> store.add(uuid, rank.id()));

        player.sendMessage(TextUtils.parse("<gold><b>UNLOCKED</b> ").append(rank.component())
                .append(Component.text(" - put it on with /ranks", NamedTextColor.GRAY)));
        return true;
    }

    public boolean revoke(Player player, Rank rank) {
        UUID uuid = player.getUniqueId();
        if (!bought.getOrDefault(uuid, Set.of()).remove(rank.id())) return false;
        db.async(() -> store.remove(uuid, rank.id()));

        // Back to the best rank their playtime has earned
        PlayerData data = players.get(player);
        if (data != null && rank.id().equals(data.getRank())) {
            Rank earned = Ranks.earnedBy(data.getPlaytime());
            data.setRank((earned == null ? Ranks.MEMBER : earned).id());
            refresh(player);
        }
        return true;
    }

    // Everywhere the rank shows
    private void refresh(Player player) {
        scoreboard.updateBoard(player);
        tab.updateAll();
    }

    // A new playtime rank is put on if the player still wears MEMBER or an earlier
    // playtime rank. A shop rank they chose is left alone, they are only told
    private void countPlaytime() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = players.get(player);
            if (data == null) continue;

            long was = data.getPlaytime();
            Rank before = Ranks.earnedBy(was);
            data.addPlaytime(PLAYTIME_SECONDS);
            onPlaytime.passed(player, was, data.getPlaytime());
            Rank after = Ranks.earnedBy(data.getPlaytime());
            if (after == null || after == before) continue;

            Rank.Kind worn = Ranks.get(data.getRank()).kind();
            boolean wear = worn == Rank.Kind.DEFAULT || worn == Rank.Kind.PLAYTIME;
            if (wear) {
                data.setRank(after.id());
                refresh(player);
            }
            unlocked(player, after);
        }
    }

    public void openRanks(Player player) {
        new RankMenu(this, plugin.getRegistry()).open(player);
    }

    public void shutdown() {
        playtime.cancel();
    }

    // Every rank goes, staff and donor ranks included, and the hours played with them
    public void reset(Player player) {
        UUID uuid = player.getUniqueId();
        bought.remove(uuid);
        db.async(() -> store.deleteAll(uuid));

        PlayerData data = players.get(player);
        if (data != null) {
            data.setRank(Ranks.MEMBER.id());
            data.addPlaytime(-data.getPlaytime());
        }
        refresh(player);
    }
}
