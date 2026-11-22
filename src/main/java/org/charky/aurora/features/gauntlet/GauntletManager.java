package org.charky.aurora.features.gauntlet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.core.DataRegistry;
import org.charky.aurora.core.database.Database;
import org.charky.aurora.core.database.GauntletStore;
import org.charky.aurora.features.build.BuildManager;
import org.charky.aurora.features.cosmetic.Cosmetic;
import org.charky.aurora.features.cosmetic.CosmeticManager;
import org.charky.aurora.features.cosmetic.Cosmetics;
import org.charky.aurora.features.player.PlayerManager;
import org.charky.aurora.features.player.ScoreboardManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// The gauntlet as a game of its own beside the main one, in a world of its own that
// GauntletWorld keeps apart. This holds every player's progress, the same in every
// loadout (Shards, Cores, gear levels, talents, best floor, counted stats), what runs
// bank and milestones pay into the main game (Essence and cosmetics), and what the
// forge, the talents and the Shard Shop sell. The systems around it are classes of
// their own, reached from here: runs, the setup, the PvP zone, duels, spectating,
// parties, daily challenges, the weekly boards, achievements and the sidebar
public class GauntletManager {
    private final Aurora plugin;
    private final Database db;
    private final GauntletStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final CosmeticManager cosmetics;
    private final BuildManager build;
    private final GauntletArena arena;
    private final Map<UUID, GauntletProfile> profiles = new ConcurrentHashMap<>();
    private GauntletRuns runs;
    private GauntletSetup setup;
    private GauntletPvp pvp;
    private GauntletDuels duels;
    private final GauntletChallenges challenges = new GauntletChallenges(this);
    private final GauntletWeekly weekly;
    private final GauntletBoards boards;
    private final GauntletTower tower;
    private final GauntletAchievements achievements = new GauntletAchievements(this);
    private GauntletSpectate spectate;
    private GauntletParty party;
    private GauntletWorld world;
    private BukkitTask sweep;
    private int ticks;

    public GauntletManager(Aurora plugin, Database db, GauntletStore store, PlayerManager players,
                           ScoreboardManager scoreboard, CosmeticManager cosmetics, BuildManager build) {
        this.plugin = plugin;
        this.db = db;
        this.store = store;
        this.players = players;
        this.scoreboard = scoreboard;
        this.cosmetics = cosmetics;
        this.build = build;
        this.arena = new GauntletArena(db, store, plugin.getDataFolder(), plugin.getLogger());
        this.weekly = new GauntletWeekly(this, plugin.getDataFolder(), plugin.getLogger());
        this.boards = new GauntletBoards(plugin, this);
        this.tower = new GauntletTower(this, plugin.getDataFolder(), plugin.getLogger());
        profiles.putAll(store.loadAll());
    }

    public void setRuns(GauntletRuns runs) {
        this.runs = runs;
    }

    public void setSetup(GauntletSetup setup) {
        this.setup = setup;
    }

    public GauntletSetup setup() {
        return setup;
    }

    public GauntletChallenges challenges() {
        return challenges;
    }

    public GauntletWeekly weekly() {
        return weekly;
    }

    GauntletBoards boards() {
        return boards;
    }

    GauntletTower tower() {
        return tower;
    }

    // To everyone in the Gauntlet's world, in a run or not
    void announce(String message) {
        Location lobby = arena.lobby();
        if (lobby == null || lobby.getWorld() == null) return;
        for (Player player : lobby.getWorld().getPlayers()) TextUtils.send(player, message);
    }

    public GauntletAchievements achievements() {
        return achievements;
    }

    public GauntletRuns runs() {
        return runs;
    }

    public void setSpectate(GauntletSpectate spectate) {
        this.spectate = spectate;
    }

    public GauntletSpectate spectate() {
        return spectate;
    }

    public void setParty(GauntletParty party) {
        this.party = party;
    }

    public GauntletParty party() {
        return party;
    }

    public void setWorld(GauntletWorld world) {
        this.world = world;
    }

    public boolean isWatching(Player player) {
        return spectate != null && spectate.isWatching(player);
    }

    public boolean ownsCosmetic(Player player, Cosmetic cosmetic) {
        return cosmetics.owns(player, cosmetic);
    }

    // A Shard Shop cosmetic. Null if it worked, otherwise why not
    public String buyCosmetic(Player player, GauntletData.ShopItem item) {
        Cosmetic cosmetic = Cosmetics.get(item.cosmetic());
        if (cosmetic == null) return "That cosmetic does not exist";
        if (cosmetics.owns(player, cosmetic)) return "You already own it";
        String unpaid = profile(player).pay(item.shards(), 0);
        if (unpaid != null) return unpaid;
        save(player.getUniqueId());
        cosmetics.give(player.getUniqueId(), cosmetic.id());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        refreshBoard(player);
        return null;
    }

    // An achievement's reward: Essence into the main game, Shards, a cosmetic
    void reward(Player player, int essence, int shards, String cosmetic) {
        if (essence > 0) players.addEssence(player, essence);
        if (shards > 0) {
            profile(player).shards += shards;
            save(player.getUniqueId());
        }
        if (cosmetic != null && !cosmetic.isBlank()) cosmetics.give(player.getUniqueId(), cosmetic);
        refreshBoard(player);
    }

    public void setDuels(GauntletDuels duels) {
        this.duels = duels;
    }

    public GauntletDuels duels() {
        return duels;
    }

    public boolean inDuel(Player player) {
        return duels != null && duels.inDuel(player);
    }

    public void setPvp(GauntletPvp pvp) {
        this.pvp = pvp;
    }

    public GauntletPvp pvp() {
        return pvp;
    }

    public boolean inRun(Player player) {
        return runs != null && runs.inRun(player);
    }

    // Leaves the run for the lobby, keeping what it earned
    public void endRun(Player player) {
        if (runs != null) runs.end(player, GauntletRuns.End.LEFT);
    }

    // The Gauntlet's menu, from /gauntlet and the menu item
    public void openMenu(Player player) {
        if (runs != null) new GauntletMenu(this, runs).open(player);
    }

    public void start() {
        sweep = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (world != null) world.everySecond();
            if (pvp != null) pvp.markBounties();
            // The weekly boards pay out once the week has turned, the lobby's boards catch up
            if (ticks % 60 == 0) {
                weekly.check();
                tower.check();
            }
            if (ticks % 30 == 0) boards.refresh();
            ticks++;
        }, 20L, 20L);
        scoreboard.setOverride(new GauntletSidebar(this)::lines);
    }

    public void refreshBoard(Player player) {
        scoreboard.updateBoard(player);
    }

    public void shutdown() {
        if (sweep != null) sweep.cancel();
    }

    public GauntletArena arena() {
        return arena;
    }

    public GauntletData data() {
        return plugin.getRegistry().getGauntlet();
    }

    public DataRegistry registry() {
        return plugin.getRegistry();
    }

    // The gauntlet's world, and not while it is a build world being worked on
    public boolean isHere(Player player) {
        return arena.isGauntletWorld(player.getWorld()) && !build.isBuildWorld(player.getWorld());
    }

    // Made on first use, with the starter gear at level 0
    public GauntletProfile profile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, id -> {
            GauntletProfile profile = new GauntletProfile();
            for (GauntletData.Gear gear : data().gear().values()) {
                if (gear.starter()) profile.gear.put(gear.id(), 0);
            }
            db.async(() -> {
                store.save(id, profile);
                profile.gear.forEach((gear, level) -> store.saveGear(id, gear, level));
            });
            return profile;
        });
    }

    public GauntletProfile profile(Player player) {
        GauntletProfile profile = profile(player.getUniqueId());
        // Starter gear added to the data after a player started is theirs too
        for (GauntletData.Gear gear : data().gear().values()) {
            if (gear.starter() && !profile.owns(gear.id())) setLevel(player.getUniqueId(), profile, gear.id(), 0);
        }
        return profile;
    }

    // Back to a new player's: Shards, Cores, best floor, runs, gear, care package,
    // starting kit, layout and milestones all go. A run in progress ends first, and
    // what milestones already paid (Essence, cosmetics) is kept
    public void reset(Player player) {
        if (runs != null) runs.end(player, GauntletRuns.End.LEFT);
        UUID uuid = player.getUniqueId();
        profiles.remove(uuid);
        db.async(() -> {
            store.reset(uuid);
            // The new profile's starter gear, saved after the rows above are gone
            GauntletProfile fresh = profiles.get(uuid);
            if (fresh != null) {
                store.save(uuid, fresh);
                fresh.gear.forEach((gear, level) -> store.saveGear(uuid, gear, level));
            }
        });
        profile(player);
        if (isHere(player)) giveKit(player);
        refreshBoard(player);
    }

    // ---- Stats and Shards ----

    // Adds to a counted stat, returns its new value
    public int addStat(UUID uuid, String stat, int amount) {
        int value = profile(uuid).stats.merge(stat, amount, Integer::sum);
        db.async(() -> store.saveStat(uuid, stat, value));
        if (!stat.startsWith(GauntletAchievements.DONE)) achievements.check(uuid);
        return value;
    }

    public void setStat(UUID uuid, String stat, int value) {
        profile(uuid).stats.put(stat, value);
        db.async(() -> store.saveStat(uuid, stat, value));
        if (!stat.startsWith(GauntletAchievements.DONE)) achievements.check(uuid);
    }

    // A finished run into the player's history
    void recordRun(UUID uuid, RunRecord run) {
        profile(uuid).record(run);
        db.async(() -> store.saveRun(uuid, run));
    }

    // Shards paid straight into the bank, not through a run
    public void giveShards(Player player, int amount) {
        profile(player).shards += amount;
        save(player.getUniqueId());
        refreshBoard(player);
    }

    // The highest of a counted stat, highest first
    // The lowest of a counted stat that is set, lowest first: the fastest times
    public List<Map.Entry<UUID, GauntletProfile>> lowestBy(String stat, int count) {
        return profiles.entrySet().stream().filter(entry -> entry.getValue().stat(stat) > 0)
                .sorted(Comparator.comparingInt((Map.Entry<UUID, GauntletProfile> entry) -> entry.getValue().stat(stat)))
                .limit(count).toList();
    }

    public List<Map.Entry<UUID, GauntletProfile>> topBy(String stat, int count) {
        return profiles.entrySet().stream().filter(entry -> entry.getValue().stat(stat) > 0)
                .sorted(Comparator.comparingInt((Map.Entry<UUID, GauntletProfile> entry) -> entry.getValue().stat(stat)).reversed())
                .limit(count).toList();
    }

    void save(UUID uuid) {
        GauntletProfile profile = profiles.get(uuid);
        if (profile != null) db.async(() -> store.save(uuid, profile));
    }

    private void setLevel(UUID uuid, GauntletProfile profile, String gear, int level) {
        profile.gear.put(gear, level);
        db.async(() -> store.saveGear(uuid, gear, level));
    }

    // The best floors, highest first
    public List<Map.Entry<UUID, GauntletProfile>> top(int count) {
        return profiles.entrySet().stream().filter(entry -> entry.getValue().best > 0)
                .sorted(Comparator.comparingInt((Map.Entry<UUID, GauntletProfile> entry) -> entry.getValue().best).reversed())
                .limit(count).toList();
    }

    public static String nameOf(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? "?" : player.getName();
    }

    // ---- Coming and going ----

    // From anywhere: to the lobby. Null if it worked, otherwise why not
    public String sendToLobby(Player player) {
        return world.sendToLobby(player);
    }

    // Back to the main spawn, ending a run
    public void leave(Player player) {
        world.leave(player);
    }

    // ---- Gear ----

    // The player's setup, as they laid it out in the lobby: gear, starting kit and
    // the menu item. Everything else in the inventory goes
    public void giveKit(Player player) {
        if (setup != null) setup.restore(player);
    }

    // Null if it worked, otherwise why not
    public String unlock(Player player, String gearId) {
        GauntletData.Gear gear = data().gear().get(gearId);
        GauntletProfile profile = profile(player);
        if (gear == null) return "There is no such gear";
        if (profile.owns(gearId)) return "You already have it";
        if (profile.best < gear.unlockFloor()) return "Reach floor " + gear.unlockFloor() + " first";
        String unpaid = profile.pay(gear.unlockShards(), 0);
        if (unpaid != null) return unpaid;
        setLevel(player.getUniqueId(), profile, gearId, 0);
        save(player.getUniqueId());
        afterPurchase(player, "<green>Unlocked the " + gear.name());
        return null;
    }

    public String upgrade(Player player, String gearId) {
        GauntletData.Gear gear = data().gear().get(gearId);
        GauntletProfile profile = profile(player);
        if (gear == null || !profile.owns(gearId)) return "Unlock it first";
        GauntletData.GearLevel next = data().level(profile.level(gearId) + 1);
        if (next == null) return "It is fully upgraded";
        String unpaid = profile.pay(next.shards(), next.cores());
        if (unpaid != null) return unpaid;
        setLevel(player.getUniqueId(), profile, gearId, next.level());
        save(player.getUniqueId());
        afterPurchase(player, "<green>" + gear.name() + " upgraded to <white>(" + next.level() + ")");
        return null;
    }

    // A talent's next level. Null if it worked, otherwise why not
    public String buyTalent(Player player, String talentId) {
        GauntletData.Talent talent = data().talents().get(talentId);
        if (talent == null) return "There is no such talent";
        if (inRun(player)) return "Talents can't be learned during a run";
        GauntletProfile profile = profile(player);
        int level = profile.talent(talentId);
        if (level >= talent.max()) return "You have learned all of it";
        int next = level + 1;
        String unpaid = profile.pay(talent.shardsFor(next), talent.coresFor(next));
        if (unpaid != null) return unpaid;
        profile.talents.put(talentId, next);
        UUID uuid = player.getUniqueId();
        db.async(() -> store.saveTalent(uuid, talentId, next));
        save(uuid);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        TextUtils.send(player, "<green>" + talent.name() + (talent.max() > 1 ? " level " + next : "") + " learned");
        refreshBoard(player);
        return null;
    }

    public String upgradeCare(Player player) {
        GauntletProfile profile = profile(player);
        if (profile.care >= data().maxCareLevel()) return "Your care package is fully upgraded";
        GauntletData.CareLevel next = data().careLevel(profile.care + 1);
        String unpaid = profile.pay(next.shards(), next.cores());
        if (unpaid != null) return unpaid;
        profile.care = next.level();
        save(player.getUniqueId());
        afterPurchase(player, "<green>Care package upgraded to level <white>" + next.level());
        return null;
    }

    private void afterPurchase(Player player, String message) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.2f);
        TextUtils.send(player, message);
        if (isHere(player) && (runs == null || !runs.inRun(player))) giveKit(player);
        refreshBoard(player);
    }

    // ---- After a run ----

    // What a run earned, and the best floor cleared in it
    void bank(Player player, int shards, int cores, int cleared) {
        UUID uuid = player.getUniqueId();
        GauntletProfile profile = profile(player);
        profile.shards += shards;
        profile.cores += cores;
        profile.runs++;
        boolean record = cleared > profile.best;
        if (record) profile.best = cleared;
        if (cleared > 0) weekly.floorCleared(uuid, cleared);
        save(uuid);
        if (record) milestones(player, profile);
        achievements.check(uuid);
        refreshBoard(player);
    }

    private void milestones(Player player, GauntletProfile profile) {
        for (GauntletData.Milestone milestone : data().milestones()) {
            if (profile.best < milestone.floor() || profile.milestones.contains(milestone.floor())) continue;
            profile.milestones.add(milestone.floor());
            db.async(() -> store.saveMilestone(player.getUniqueId(), milestone.floor()));

            if (milestone.essence() > 0) players.addEssence(player, milestone.essence());
            if (!milestone.cosmetic().isBlank()) cosmetics.give(player.getUniqueId(), milestone.cosmetic());
            scoreboard.updateBoard(player);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
            TextUtils.send(player, "<gold><b>GAUNTLET MILESTONE</b> <white>Floor " + milestone.floor()
                    + (milestone.essence() > 0 ? " <aqua>+" + milestone.essence() + " Essence" : ""));
        }
    }
}
