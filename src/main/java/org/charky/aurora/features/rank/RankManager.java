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
import java.util.function.BiPredicate;

// Which ranks a player owns and which one they wear. Everyone has MEMBER, playtime
// ranks come with hours online, shop ranks are bought with Essence. The worn rank
// shows in chat, the tab list and the sidebar, and changes nothing else. Zone and
// world ranks come with maxing out a zone or a world. Donor and staff ranks are
// handed out by an admin, and so is the second slot, which shows a second rank
// after the first
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
    // The rank worn in the second slot
    private final Map<UUID, String> second;
    private final BukkitTask playtime;
    // Whether a player has maxed out the zone or world of a rank. Tools load after
    // ranks, so it is set once they have
    private BiPredicate<Player, Rank> maxed = (player, rank) -> false;

    public RankManager(Aurora plugin, Database db, RankStore store, PlayerManager players,
                       ScoreboardManager scoreboard, TabManager tab) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.tab = tab;
        this.bought = store.loadAll();
        this.second = store.loadSecond();
        tab.setRanks(this::prefix);
        this.playtime = Bukkit.getScheduler().runTaskTimer(plugin, this::countPlaytime, PLAYTIME_TICKS, PLAYTIME_TICKS);
    }

    // The rank a player wears, MEMBER while their profile is still loading
    public Rank worn(Player player) {
        PlayerData data = players.get(player);
        return data == null ? Ranks.MEMBER : Ranks.get(data.getRank());
    }

    public void setMaxed(BiPredicate<Player, Rank> maxed) {
        this.maxed = maxed;
    }

    public boolean hasSecondSlot(Player player) {
        return bought.getOrDefault(player.getUniqueId(), Set.of()).contains(Ranks.SECOND_SLOT);
    }

    // The rank in the second slot, null for none
    public Rank wornSecond(Player player) {
        String id = second.get(player.getUniqueId());
        return id == null || !hasSecondSlot(player) ? null : Ranks.get(id);
    }

    // What goes before a name: the rank, then the second one if there is one
    public Component prefix(Player player) {
        Component prefix = worn(player).component();
        Rank other = wornSecond(player);
        return other == null ? prefix : prefix.append(Component.space()).append(other.component());
    }

    // Null if it is now worn second, otherwise why not
    public String wearSecond(Player player, Rank rank) {
        if (!hasSecondSlot(player)) return "A second rank comes with a donor package";
        if (!owns(player, rank)) return "You have not unlocked that rank";
        if (rank == worn(player)) return "You already wear that one first";

        setSecond(player, rank.id());
        return null;
    }

    public void takeOffSecond(Player player) {
        setSecond(player, null);
    }

    private void setSecond(Player player, String id) {
        UUID uuid = player.getUniqueId();
        if (id == null) second.remove(uuid);
        else second.put(uuid, id);
        db.async(() -> store.setSecond(uuid, id));
        refresh(player);
    }

    // For admins, the only way to a second slot. True if it changed anything
    public boolean setSecondSlot(Player player, boolean has) {
        UUID uuid = player.getUniqueId();
        if (has) {
            if (!bought.computeIfAbsent(uuid, k -> new HashSet<>()).add(Ranks.SECOND_SLOT)) return false;
            db.async(() -> store.add(uuid, Ranks.SECOND_SLOT));
            player.sendMessage(TextUtils.parse("<gold><b>UNLOCKED</b> <white>A second rank <gray>- right click one in /ranks"));
            return true;
        }
        if (!bought.getOrDefault(uuid, Set.of()).remove(Ranks.SECOND_SLOT)) return false;
        db.async(() -> store.remove(uuid, Ranks.SECOND_SLOT));
        takeOffSecond(player);
        return true;
    }

    // Told once, when the upgrade that finishes a zone is bought. The rank itself
    // is never stored, it follows from the levels
    public void announceMaxed(Player player, String zoneId, String region) {
        Rank zone = Ranks.earnedIn(Rank.Kind.ZONE, zoneId);
        if (zone != null) unlocked(player, zone);

        Rank world = Ranks.earnedIn(Rank.Kind.WORLD, region);
        if (world != null && owns(player, world)) unlocked(player, world);
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
            case ZONE, WORLD -> maxed.test(player, rank);
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

        // The same rank twice would only repeat itself
        if (rank == wornSecond(player)) second.remove(player.getUniqueId());
        data.setRank(rank.id());
        setSecond(player, second.get(player.getUniqueId()));
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

        if (rank.id().equals(second.get(uuid))) takeOffSecond(player);

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

            Rank before = Ranks.earnedBy(data.getPlaytime());
            data.addPlaytime(PLAYTIME_SECONDS);
            Rank after = Ranks.earnedBy(data.getPlaytime());
            if (after == null || after == before) continue;

            Rank.Kind worn = Ranks.get(data.getRank()).kind();
            boolean wear = worn == Rank.Kind.DEFAULT || worn == Rank.Kind.PLAYTIME;
            if (wear) {
                data.setRank(after.id());
                refresh(player);
            }
            player.sendMessage(TextUtils.parse("<gold><b>RANK UP</b> <gray>Your time here has earned you ").append(after.component())
                    .append(Component.text(wear ? "" : " - put it on with /ranks", NamedTextColor.GRAY)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
        }
    }

    public void openRanks(Player player) {
        new RankMenu(this, plugin.getRegistry()).open(player);
    }

    public void shutdown() {
        playtime.cancel();
    }

    // Every rank and the second slot go, staff and donor ranks included, and the
    // hours played with them
    public void reset(Player player) {
        UUID uuid = player.getUniqueId();
        bought.remove(uuid);
        second.remove(uuid);
        db.async(() -> store.deleteAll(uuid));

        PlayerData data = players.get(player);
        if (data != null) {
            data.setRank(Ranks.MEMBER.id());
            data.addPlaytime(-data.getPlaytime());
        }
        refresh(player);
    }
}
