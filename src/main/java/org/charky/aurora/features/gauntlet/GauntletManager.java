package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.DataKeys;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// The gauntlet as a game of its own beside the main one, in its own world. Walking in
// puts the player's inventory away and hands out their gauntlet setup (GauntletSetup);
// walking out gives it back, so nothing crosses over either way. What carries over is the gauntlet's own
// progress (Shards, Cores, gear levels, best floor) and what milestones pay into the
// main game: Essence and cosmetics. Gear is bought and upgraded in the forge with
// Shards and Cores from runs (GauntletRuns). PvP is off in the world except inside
// its PvP zone, where players fight with their gauntlet gear
public class GauntletManager implements Listener {
    private final Aurora plugin;
    private final Database db;
    private final GauntletStore store;
    private final PlayerManager players;
    private final ScoreboardManager scoreboard;
    private final CosmeticManager cosmetics;
    private final BuildManager build;
    private final GauntletArena arena;
    private final Map<UUID, GauntletProfile> profiles = new ConcurrentHashMap<>();
    // Died in the gauntlet's world, to come back in its lobby
    private final Set<UUID> diedHere = new HashSet<>();
    private GauntletRuns runs;
    private GauntletSetup setup;
    private GauntletPvp pvp;
    private GauntletDuels duels;
    private final GauntletChallenges challenges = new GauntletChallenges(this);
    private final GauntletWeekly weekly;
    private final GauntletAchievements achievements = new GauntletAchievements(this);
    private GauntletSpectate spectate;
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
        GauntletProfile profile = profile(player);
        if (profile.shards < item.shards()) return "You need " + (item.shards() - profile.shards) + " more Shards";
        profile.shards -= item.shards();
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
            sweepAll();
            hintNewPlayers();
            // The weekly boards pay out once the week has turned
            if (ticks++ % 60 == 0) weekly.check();
        }, 20L, 20L);
        scoreboard.setOverride(this::sidebar);
    }

    // In the Gauntlet's world the sidebar shows its Shards, Cores and best floor, and
    // the run in progress. Elsewhere the main game's
    private List<Component> sidebar(Player player) {
        if (!isHere(player)) return null;
        GauntletProfile profile = profile(player);
        List<Component> lines = new ArrayList<>(List.of(
                TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>GAUNTLET</b></gradient>"),
                TextUtils.parse("<gray>• SHARDS: <aqua>" + profile.shards),
                TextUtils.parse("<gray>• CORES: <light_purple>" + profile.cores),
                TextUtils.parse("<gray>• BEST FLOOR: <white>" + profile.best)));
        Player watched = spectate == null ? null : spectate.target(player);
        Player opponent = duels == null ? null : duels.opponent(player);
        if (watched != null) {
            lines.add(Component.empty());
            lines.add(TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>SPECTATING</b></gradient>"));
            lines.add(TextUtils.parse("<gray>• PLAYER: <white>" + watched.getName()));
            lines.add(TextUtils.parse(runs != null && runs.inRun(watched)
                    ? "<gray>• FLOOR: <white>" + runs.floorOf(watched) + " <gray>SHARDS: <aqua>" + runs.shardsOf(watched)
                    : "<gray>• IN A DUEL"));
        } else if (opponent != null) {
            lines.add(Component.empty());
            lines.add(TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>DUEL</b></gradient>"));
            lines.add(TextUtils.parse("<gray>• VS: <white>" + opponent.getName()));
            lines.add(TextUtils.parse("<gray>• THEIR HEALTH: <red>" + Math.round(opponent.getHealth()) / 2.0 + " ❤"));
        } else if (pvp != null && pvp.inZone(player)) {
            lines.add(Component.empty());
            lines.add(TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>PVP</b></gradient>"));
            lines.add(TextUtils.parse("<gray>• KILLS: <red>" + profile.stat(GauntletPvp.KILLS)
                    + " <gray>DEATHS: <white>" + profile.stat(GauntletPvp.DEATHS)));
            lines.add(TextUtils.parse("<gray>• STREAK: <gold>" + pvp.streak(player)
                    + " <gray>BEST: <white>" + profile.stat(GauntletPvp.BEST_STREAK)));
        } else if (runs != null && runs.inRun(player)) {
            lines.add(Component.empty());
            lines.add(TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>THIS RUN</b></gradient>"));
            lines.add(TextUtils.parse("<gray>• FLOOR: <white>" + runs.floorOf(player)));
            lines.add(TextUtils.parse("<gray>• EARNED: <aqua>" + runs.shardsOf(player) + " Shards"
                    + (runs.coresOf(player) > 0 ? " <light_purple>" + runs.coresOf(player) + " Cores" : "")));
        } else {
            lines.add(TextUtils.parse("<gray>• RUNS: <white>" + profile.runs));
        }
        return lines;
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

    // Shards paid straight into the bank, not through a run
    public void giveShards(Player player, int amount) {
        profile(player).shards += amount;
        save(player.getUniqueId());
        refreshBoard(player);
    }

    // The highest of a counted stat, highest first
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
        Location lobby = arena.lobby();
        if (lobby == null) return "The Gauntlet is not open yet";
        if (!data().ready()) return "The Gauntlet's data did not load";
        player.teleport(lobby);
        return null;
    }

    public void leave(Player player) {
        if (runs != null) runs.end(player, GauntletRuns.End.LEFT);
        Location spawn = plugin.getMainSpawn();
        player.teleport(spawn != null ? spawn : Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    // Before the loadouts swap (NORMAL), so a loadout put away on the way out holds
    // the player's own inventory, not their gauntlet gear
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeaveWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!arena.isGauntletWorld(event.getFrom()) || build.isBuildWorld(event.getFrom())) return;
        if (isHere(player)) return;
        if (runs != null) runs.end(player, GauntletRuns.End.LEFT);
        giveBack(player);
        refreshBoard(player);
    }

    // After the loadouts swap, so what is put away is the loadout coming in
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnterWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!isHere(player) || (arena.isGauntletWorld(event.getFrom()) && !build.isBuildWorld(event.getFrom()))) return;
        putAway(player);
        giveKit(player);
        refreshBoard(player);
        welcome(player);
    }

    // A player who logged out here comes back in the lobby, never inside an arena copy.
    // One who is somewhere else with an inventory still put away gets it back
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            weekly.notice(player);
            achievements.check(player.getUniqueId());
            if (isHere(player)) {
                Location lobby = arena.lobby();
                if (lobby != null) player.teleport(lobby);
                giveKit(player);
                welcome(player);
            } else if (!build.isBuildWorld(player.getWorld())) {
                giveBack(player);
            }
        }, 5L);
    }

    // How to get going, the moment they arrive: a title, and buttons in chat
    private void welcome(Player player) {
        String how = arena.hasStart() ? "Walk into the portal to start a run" : "Type /gauntlet start to start a run";
        player.showTitle(Title.title(TextUtils.parse("<gradient:#FF416C:#FF4B2B><b>THE GAUNTLET</b></gradient>"),
                TextUtils.parse("<gray>" + how)));
        TextUtils.send(player, "");
        TextUtils.send(player, "<gradient:#FF416C:#FF4B2B><b>THE GAUNTLET</b></gradient> <gray>Climb the tower floor by floor, "
                + "spend Shards on your gear, climb higher.");
        TextUtils.send(player, "<gray>" + how + ", or click: "
                + "<click:run_command:'/gauntlet start'><hover:show_text:'<gray>Start a run now'><green><b>[START A RUN]</b></hover></click> "
                + "<click:run_command:'/gauntlet forge'><hover:show_text:'<gray>Upgrade your gear'><gold><b>[FORGE]</b></hover></click> "
                + "<click:run_command:'/gauntlet'><hover:show_text:'<gray>Everything else'><yellow><b>[MENU]</b></hover></click>");
        int done = challenges.doneToday(player);
        int total = challenges.today().size();
        if (total > 0) TextUtils.send(player, "<gray>Daily challenges: <white>" + done + "/" + total + "</white> done "
                + "<click:run_command:'/gauntlet'><hover:show_text:'<gray>See them in the menu'><yellow>[SEE]</hover></click>");
        TextUtils.send(player, "<dark_gray>Your own inventory is kept for when you leave, /gauntlet leave.");
        TextUtils.send(player, "");
    }

    // Until their first run, players standing in the lobby are told how to start
    private void hintNewPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isHere(player) || inRun(player) || isWatching(player) || profile(player).runs > 0) continue;
            if (pvp != null && pvp.inZone(player)) continue;
            player.sendActionBar(TextUtils.parse(arena.hasStart()
                    ? "<green><b>Walk into the portal</b> <gray>to start your first run"
                    : "<green><b>/gauntlet start</b> <gray>to start your first run"));
        }
    }

    // Walking into the start portal starts a run
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        Player player = event.getPlayer();
        if (!isHere(player) || inRun(player) || !arena.inStart(to) || arena.inStart(from) || runs == null) return;
        String problem = runs.startFlow(player);
        if (problem != null) TextUtils.error(player, problem);
    }

    private void putAway(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] contents = Arrays.stream(player.getInventory().getContents())
                .map(item -> item == null ? ItemStack.empty() : item).toArray(ItemStack[]::new);
        byte[] items = ItemStack.serializeItemsAsBytes(contents);
        // Saved before the inventory is cleared, a crash in between loses nothing
        store.saveInventory(uuid, items);
        player.getInventory().clear();
    }

    private void giveBack(Player player) {
        UUID uuid = player.getUniqueId();
        byte[] items = store.loadInventory(uuid);
        if (items == null) return;
        player.getInventory().clear();
        player.getInventory().setContents(ItemStack.deserializeItemsFromBytes(items));
        player.updateInventory();
        db.async(() -> store.removeInventory(uuid));
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
        if (profile.shards < gear.unlockShards()) return "You need " + (gear.unlockShards() - profile.shards) + " more Shards";
        profile.shards -= gear.unlockShards();
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
        if (profile.shards < next.shards()) return "You need " + (next.shards() - profile.shards) + " more Shards";
        if (profile.cores < next.cores()) return "You need " + (next.cores() - profile.cores) + " more Cores";
        profile.shards -= next.shards();
        profile.cores -= next.cores();
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
        if (profile.shards < talent.shardsFor(next)) return "You need " + (talent.shardsFor(next) - profile.shards) + " more Shards";
        if (profile.cores < talent.coresFor(next)) return "You need " + (talent.coresFor(next) - profile.cores) + " more Cores";
        profile.shards -= talent.shardsFor(next);
        profile.cores -= talent.coresFor(next);
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
        if (profile.shards < next.shards()) return "You need " + (next.shards() - profile.shards) + " more Shards";
        if (profile.cores < next.cores()) return "You need " + (next.cores() - profile.cores) + " more Cores";
        profile.shards -= next.shards();
        profile.cores -= next.cores();
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

    // ---- Keeping the world apart ----

    // Gear from the main game would carry its stats into the gauntlet (taken out of
    // the Armory here, say), so anything with Aurora stats that is not gauntlet gear
    // is taken away, as are main-game tools
    private void sweepAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isHere(player)) continue;
            PlayerInventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack item = inventory.getItem(slot);
                if (item == null || !item.hasItemMeta() || GauntletGear.gearId(item) != null) continue;
                var pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(DataKeys.ENCHANT_KEY, PersistentDataType.TAG_CONTAINER)
                        || pdc.has(DataKeys.TOOL_KEY, PersistentDataType.STRING)) {
                    inventory.setItem(slot, null);
                }
            }
        }
    }

    // The ender chest holds main-game things
    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isHere(player)
                && event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            TextUtils.error(player, "Your ender chest can't be opened in the Gauntlet");
        }
    }

    // Players only hurt each other in the PvP zone, both in it and neither in a run
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !isHere(victim)) return;
        Player attacker = event.getDamager() instanceof Player p ? p
                : event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if (attacker == null) return;
        // Duelists fight each other and nobody else
        if (duels != null && duels.areDueling(victim, attacker)) return;
        String refused = null;
        if (inDuel(victim) || inDuel(attacker)) refused = inDuel(attacker) ? "You can only hit your duel opponent" : victim.getName() + " is in a duel";
        else if (inRun(victim) || inRun(attacker)) refused = "No PvP during a run";
        else if (!arena.inPvp(attacker.getLocation())) refused = "You can only fight players in the PvP zone";
        else if (!arena.inPvp(victim.getLocation())) refused = victim.getName() + " is outside the PvP zone";
        if (refused == null) return;
        event.setCancelled(true);
        attacker.sendActionBar(TextUtils.parse("<red>" + refused));
    }

    // Dying here costs nothing, the gear is kept, and they come back in the lobby
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!isHere(player)) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        diedHere.add(player.getUniqueId());
    }

    // Back in the lobby with their setup as they left it, whatever the fight used up
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!diedHere.remove(player.getUniqueId())) return;
        Location lobby = arena.lobby();
        if (lobby != null) event.setRespawnLocation(lobby);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && isHere(player)) giveKit(player);
        });
    }
}
