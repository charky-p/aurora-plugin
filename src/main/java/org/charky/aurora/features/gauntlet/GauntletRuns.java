package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.combat.CustomHealth;
import org.charky.aurora.features.gauntlet.GauntletRun.Member;
import org.charky.aurora.features.gauntlet.GauntletRun.State;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.spawner.SpawnerManager;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Runs up the tower, each in its own copy of the arena, alone or with a party (the
// leader starts it for everyone). Floor after floor (GauntletWaves brings each wave):
// every kill pays Shards into the party's pot at once, clearing a floor a bonus (more
// under its par time), boss floors Cores. Every boon_every floors a care package lands
// and every player picks their own boon; altars offer pacts. A hit that would kill
// (unless a totem or a Second Wind saves them) puts a player down until the floor is
// cleared, and the run ends when nobody is standing. Everyone banks the pot with their
// own Greed and day's first run. Veterans can start past the boss floors the whole
// party has cleared, and at a heat they have unlocked. GauntletBonuses adds up boons,
// perks and talents, GauntletRunCombat puts them into hits
public class GauntletRuns implements Listener {
    public enum End { DIED, LEFT, QUIT }

    private static final long FLOOR_DELAY_TICKS = 60L;
    // How close to where a teammate went down a player sneaks to bring them back
    private static final double REVIVE_REACH = 2;
    private static final String DAILY_DAY = "daily_day";
    private static final String HEAT_CHOICE = "heat_choice";
    public static final String BEST_COMBO = "best_combo";
    // A player's kills of one mob, slain:g_rotter, and how many mobs they have met
    static final String SLAIN = "slain:";
    static final String DISCOVERED = "discovered";
    // A player's fastest solo run from floor 1 to sprint_floor, in seconds
    static final String SPRINT = "sprint";

    private final Aurora plugin;
    private final GauntletManager manager;
    private final GauntletBonuses bonuses;
    private final GauntletWaves waves;
    private final GauntletCarePackage care;
    // Every player in a run, to the run they share
    private final Map<UUID, GauntletRun> runs = new HashMap<>();
    private BukkitTask task;
    // The plugin is disabling: runs only end, nothing moves on to a next floor
    private boolean stopping;

    public GauntletRuns(Aurora plugin, GauntletManager manager, SpawnerManager spawners) {
        this.plugin = plugin;
        this.manager = manager;
        this.bonuses = new GauntletBonuses(manager);
        GauntletRunCombat combat = new GauntletRunCombat(manager, this, bonuses);
        this.waves = new GauntletWaves(plugin, manager, spawners, combat);
        this.care = new GauntletCarePackage(manager, bonuses);
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        stopping = true;
        for (Player player : plugin.getServer().getOnlinePlayers()) end(player, End.LEFT);
    }

    public boolean inRun(Player player) {
        return runs.containsKey(player.getUniqueId());
    }

    GauntletRun runOf(Player player) {
        return runs.get(player.getUniqueId());
    }

    Member memberOf(Player player) {
        GauntletRun run = runs.get(player.getUniqueId());
        return run == null ? null : run.members.get(player.getUniqueId());
    }

    // For the sidebar, 0 outside a run
    int floorOf(Player player) {
        GauntletRun run = runOf(player);
        return run == null ? 0 : run.floor;
    }

    int shardsOf(Player player) {
        GauntletRun run = runOf(player);
        return run == null ? 0 : run.shards;
    }

    int coresOf(Player player) {
        GauntletRun run = runOf(player);
        return run == null ? 0 : run.cores;
    }

    // The party's health for the sidebar, empty for a solo run
    List<String> teamLines(Player player) {
        GauntletRun run = runOf(player);
        if (run == null || run.members.size() < 2) return List.of();
        List<String> lines = new ArrayList<>();
        for (Member member : run.members.values()) {
            Player mate = Bukkit.getPlayer(member.id);
            String name = GauntletManager.nameOf(member.id);
            lines.add("<gray>• " + (member.down || mate == null ? "<dark_gray>" + name + " <red>down"
                    : "<white>" + name + " <red>" + Math.round(mate.getHealth()) / 2.0 + "❤"));
        }
        return lines;
    }

    // The lowest level of the armour pieces, for the forge's set bonuses
    int armorSetLevel(GauntletProfile profile) {
        return bonuses.armorSetLevel(profile);
    }

    private GauntletData data() {
        return manager.data();
    }

    private List<Player> team(Player player) {
        return manager.party() == null ? List.of(player) : manager.party().team(player);
    }

    // ---- Starting and ending ----

    // The floors a run can start on: 1, and the floor after every boss floor the whole
    // party has cleared
    public List<Integer> checkpoints(Player player) {
        List<Integer> floors = new ArrayList<>(List.of(1));
        int best = Integer.MAX_VALUE;
        for (Player mate : team(player)) best = Math.min(best, manager.profile(mate).best);
        for (int floor = 10; floor <= best; floor += 10) floors.add(floor + 1);
        return floors;
    }

    // From the menu, the start portal and /gauntlet start: straight into floor 1, or a
    // pick of checkpoints and heat for those past a boss. Null if it went on, otherwise why not
    public String startFlow(Player player) {
        String problem = canStart(player);
        if (problem != null) return problem;
        if (checkpoints(player).size() > 1 || heatUnlocked(player) > 0) new CheckpointMenu(manager, this).open(player);
        else return begin(player, 1);
        return null;
    }

    // Heat makes a run harder and pay more; a level unlocks every few floors of the
    // best, for a party the lowest best of them all
    public int heatUnlocked(Player player) {
        int unlocked = Integer.MAX_VALUE;
        for (Player mate : team(player)) unlocked = Math.min(unlocked, data().heatUnlocked(manager.profile(mate).best));
        return unlocked == Integer.MAX_VALUE ? 0 : unlocked;
    }

    // The heat the next run starts at, never above what is unlocked
    public int heat(Player player) {
        return Math.min(manager.profile(player).stat(HEAT_CHOICE), heatUnlocked(player));
    }

    // Steps the chosen heat up, back to 0 past the highest unlocked
    void nextHeat(Player player) {
        int unlocked = heatUnlocked(player);
        int next = heat(player) >= unlocked ? 0 : heat(player) + 1;
        manager.setStat(player.getUniqueId(), HEAT_CHOICE, next);
    }

    private double heatPay(GauntletRun run) {
        return 1 + run.heat * data().setting("heat_shards", 30) / 100;
    }

    private String canBegin(Player player) {
        if (inRun(player)) return "already in a run";
        if (!manager.isHere(player)) return "not in the Gauntlet";
        if (manager.pvp() != null && manager.pvp().inZone(player)) return "in the PvP zone";
        if (manager.inDuel(player)) return "in a duel";
        if (manager.isWatching(player)) return "spectating";
        return null;
    }

    // Everyone in the party ready, and the leader asking
    private String canStart(Player player) {
        if (manager.party() != null && !manager.party().isLeader(player)) return "Only your party leader can start a run";
        for (Player mate : team(player)) {
            String problem = canBegin(mate);
            if (problem != null) return (mate.equals(player) ? "You are " : mate.getName() + " is ") + problem;
        }
        if (!data().ready()) return "The Gauntlet's data did not load";
        String missing = manager.arena().missing();
        if (missing != null) return "The arena is not set up yet (" + missing + ")";
        return null;
    }

    public String begin(Player player) {
        return begin(player, 1);
    }

    // Null if it started, otherwise why not
    public String begin(Player player, int startFloor) {
        String problem = canStart(player);
        if (problem != null) return problem;
        if (!checkpoints(player).contains(startFloor)) return "Your party can't start on floor " + startFloor + " yet";

        List<Player> team = team(player);
        GauntletArena arena = manager.arena();
        int slot = arena.claimSlot();
        GauntletRun run = new GauntletRun(slot);
        run.partySize = team.size();
        run.startFloor = startFloor;
        run.heat = heat(player);
        run.floor = startFloor - 1;
        arena.paste(slot);
        run.bar = BossBar.bossBar(TextUtils.parse("<red><b>Floor " + startFloor), 0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        Location spawn = arena.inSlot(slot, arena.place(GauntletArena.SPAWN));
        long today = LocalDate.now().toEpochDay();
        for (Player mate : team) join(run, mate, spawn, today);
        if (team.size() > 1) run.tell("<aqua><b>PARTY RUN</b> <gray>" + team.size() + " climbing together: more mobs, tougher bosses, "
                + "one pot of Shards for everyone. A fallen player is back once the floor is cleared");
        if (run.heat > 0) run.tell("<gold><b>HEAT " + run.heat + "</b> <gray>Mobs have <white>+"
                + fmt(run.heat * data().setting("heat_health", 25)) + "%</white> health and hit <white>+"
                + fmt(run.heat * data().setting("heat_damage", 15)) + "%</white> harder, for <aqua>+"
                + fmt(run.heat * data().setting("heat_shards", 30)) + "% Shards");
        if (startFloor > 1) {
            checkpointBoons(run);
            care.drop(run);
        }
        nextFloor(run);
        return null;
    }

    // A player into the run: their own rerolls, revives and day's first run, the setup
    // from the lobby and their hearts, at the arena's spawn under the run's bar
    private void join(GauntletRun run, Player mate, Location spawn, long today) {
        GauntletProfile profile = manager.profile(mate);
        Member member = new Member(mate.getUniqueId());
        member.bestBefore = profile.best;
        member.rerolls = (int) bonuses.talentTotal(profile, "reroll");
        member.revives = (int) bonuses.talentTotal(profile, "revive");
        member.daily = profile.stat(DAILY_DAY) != today;
        if (member.daily) manager.setStat(mate.getUniqueId(), DAILY_DAY, (int) today);
        boolean first = profile.runs == 0;
        run.members.put(mate.getUniqueId(), member);
        runs.put(mate.getUniqueId(), run);

        mate.closeInventory();
        manager.giveKit(mate);
        bonuses.runHealth(mate, profile);
        GauntletPlayers.heal(mate);
        mate.teleport(spawn);
        mate.showBossBar(run.bar);
        mate.playSound(mate.getLocation(), Sound.EVENT_RAID_HORN, 0.6f, 1f);
        if (member.daily) TextUtils.send(mate, "<gold><b>DAILY BONUS</b> <gray>Your first run today pays <white>"
                + fmt(data().setting("daily_bonus", 2)) + "x</white> Shards");
        if (first) TextUtils.send(mate, "<yellow><b>TIP</b> <gray>Kill every mob to clear a floor. Every "
                + boonEvery() + " floors a care package lands and you pick a boon. If you fall, you keep the Shards you earned.");
    }

    // Starting past floor 1: everyone gets the boons of the floors skipped, at random
    private void checkpointBoons(GauntletRun run) {
        for (Member member : run.members.values()) {
            Player mate = Bukkit.getPlayer(member.id);
            List<String> given = new ArrayList<>();
            for (int i = 0; i < (run.startFloor - 1) / boonEvery(); i++) {
                List<String> pick = bonuses.pickBoons(member, 1, null);
                if (pick.isEmpty()) break;
                GauntletData.Boon boon = data().boons().get(pick.getFirst());
                grant(member, boon);
                given.add(boon.name());
            }
            if (mate == null) continue;
            bonuses.applyBoons(mate, member);
            if (!given.isEmpty()) TextUtils.send(mate, "<gold><b>CHECKPOINT</b> <gray>Starting on floor " + run.startFloor
                    + " with: <white>" + String.join(", ", given));
        }
    }

    // A player out of the run: they bank the party's pot with their own bonuses and are
    // back in the lobby with their setup. The run goes on for the rest, and ends with
    // the last of them
    public void end(Player player, End why) {
        GauntletRun run = runs.remove(player.getUniqueId());
        if (run == null) return;
        Member member = run.members.remove(player.getUniqueId());
        if (run.bar != null) player.hideBossBar(run.bar);
        if (member.down) {
            player.setGameMode(member.mode);
            clearMarker(member);
        }
        bonuses.clearBoons(player);
        GauntletCarePackage.takeRunItems(player);
        bank(player, run, member, why);

        if (run.members.isEmpty()) close(run);
        else if (!run.wiping && !stopping) {
            run.tell("<gray>" + player.getName() + " left the run");
            // Nobody left standing to clear the floor
            if (run.standing().isEmpty()) wipe(run);
            else if (run.state == State.CHOOSING) choicesDone(run);
        }
        if (why == End.QUIT) return;

        GauntletPlayers.heal(player);
        if (manager.isHere(player)) {
            Location lobby = manager.arena().lobby();
            if (lobby != null) player.teleport(lobby);
            manager.giveKit(player);
        }
    }

    // Everyone is down: the run is over for all of them
    private void wipe(GauntletRun run) {
        run.wiping = true;
        for (Player player : run.online()) end(player, End.DIED);
        for (Member member : run.members.values()) {
            runs.remove(member.id);
            clearMarker(member);
        }
        run.members.clear();
        if (!run.over) close(run);
    }

    // The last player is out: the run's mobs go and its arena copy is free again
    private void close(GauntletRun run) {
        run.over = true;
        for (UUID id : run.mobs) {
            Entity mob = Bukkit.getEntity(id);
            if (mob != null) mob.remove();
        }
        if (run.boss != null) waves.bossGone(run.boss);
        Entity hoarder = run.hoarder == null ? null : Bukkit.getEntity(run.hoarder);
        if (hoarder != null) hoarder.remove();
        manager.arena().freeSlot(run.slot);
    }

    private void bank(Player player, GauntletRun run, Member member, End why) {
        GauntletProfile profile = manager.profile(player);
        int cleared = run.cleared();
        boolean record = cleared > member.bestBefore;
        double greed = bonuses.boonTotal(member, "shards") + bonuses.talentTotal(profile, "shards");
        double daily = member.daily ? data().setting("daily_bonus", 2) : 1;
        // Everyone in a party banks the whole pot, and party_shards more for each of the others
        double party = GauntletRun.partyScale(run.partySize, data().setting("party_shards", 0));
        int banked = (int) Math.round(run.shards * (1 + greed / 100) * daily * party);
        manager.bank(player, banked, run.cores, cleared);
        UUID uuid = player.getUniqueId();
        manager.addStat(uuid, "kills", member.kills);
        manager.addStat(uuid, "elite_kills", member.eliteKills);
        manager.addStat(uuid, "boss_kills", run.bossKills);
        manager.addStat(uuid, "hoarders", run.hoarders);
        manager.addStat(uuid, "fast_clears", run.fastClears);
        if (member.bestCombo > profile.stat(BEST_COMBO)) manager.setStat(uuid, BEST_COMBO, member.bestCombo);
        long now = System.currentTimeMillis();
        manager.recordRun(uuid, new RunRecord(now, run.floor, cleared, run.startFloor, run.heat, run.partySize,
                (int) ((now - run.started) / 1000), member.kills, banked, run.cores, why == End.DIED,
                String.join(",", member.boons.entrySet().stream().map(boon -> boon.getKey() + ":" + boon.getValue()).toList())));
        if (!member.slain.isEmpty()) {
            member.slain.forEach((mob, count) -> manager.addStat(uuid, SLAIN + mob, count));
            manager.setStat(uuid, DISCOVERED, BestiaryMenu.met(data(), profile));
        }
        // A run counts for the challenge once it cleared a floor
        if (cleared >= run.startFloor) {
            manager.challenges().add(player, "runs", 1);
            if (run.partySize > 1) manager.addStat(uuid, "party_runs", 1);
        }
        if (why == End.QUIT) return;
        player.showTitle(Title.title(TextUtils.parse(why == End.DIED ? "<red><b>DEFEATED" : "<gray><b>RUN OVER"),
                TextUtils.parse(record ? "<gold><b>NEW BEST</b> <gray>Floors cleared: <white>" + cleared
                        : "<gray>Floors cleared: <white>" + cleared)));
        summary(player, run, member, why, cleared, record, greed, daily, party, banked);
        if (profile.runs == 1) {
            TextUtils.send(player, "<yellow><b>TIP</b> <gray>Spend your Shards on gear in the forge, every level hits harder: "
                    + "<click:run_command:'/gauntlet forge'><gold><b>[FORGE]</b></click>");
        }
    }

    private void summary(Player player, GauntletRun run, Member member, End why, int cleared, boolean record, double greed, double daily,
                         double partyPay, int banked) {
        long seconds = (System.currentTimeMillis() - run.started) / 1000;
        String time = clock(seconds);
        boolean party = run.partySize > 1;
        TextUtils.send(player, "");
        TextUtils.send(player, "<gradient:#FF416C:#FF4B2B><b>RUN OVER</b></gradient> <gray>" + (why == End.DIED
                ? "fell on floor <white>" + run.floor : "left on floor <white>" + run.floor)
                + (run.startFloor > 1 ? " <dark_gray>(started on " + run.startFloor + ")" : "")
                + (run.heat > 0 ? " <gold>Heat " + run.heat : ""));
        TextUtils.send(player, "<gray>Cleared <white>" + cleared + "</white> in <white>" + time + "</white>, <white>"
                + member.kills + "</white> kills (" + member.eliteKills + " elite)" + (party ? " <dark_gray>(party " + run.kills + ")" : "")
                + "<gray>, <white>" + run.fastClears + "</white> fast clears, best combo <gold>x" + member.bestCombo
                + (run.hoarders > 0 ? "<gray>, <gold>" + run.hoarders + "</gold> hoarder" + (run.hoarders > 1 ? "s" : "") + " caught" : ""));
        String bonus = (greed > 0 ? " <gray>+" + fmt(greed) + "% greed" : "") + (daily > 1 ? " <gray>x" + fmt(daily) + " daily" : "")
                + (partyPay > 1 ? " <gray>x" + fmt(partyPay) + " party" : "");
        TextUtils.send(player, "<gray>Shards: <aqua>" + run.shards + bonus + (bonus.isEmpty() ? "" : " <gray>= <aqua><b>" + banked + "</b>")
                + (run.cores > 0 ? " <gray>Cores: <light_purple>" + run.cores : ""));
        if (!member.boons.isEmpty()) {
            List<String> boons = new ArrayList<>();
            member.boons.forEach((id, stacks) -> {
                GauntletData.Boon boon = data().boons().get(id);
                if (boon != null) boons.add(boon.colour() + boon.name() + (stacks > 1 ? " x" + stacks : ""));
            });
            TextUtils.send(player, "<gray>Boons: " + String.join("<gray>, ", boons));
        }
        TextUtils.send(player, record ? "<gold><b>NEW BEST!</b> <gray>(was " + member.bestBefore + ")"
                : "<gray>Best: <white>" + manager.profile(player).best);
        TextUtils.send(player, "");
    }

    private static String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private int boonEvery() {
        return Math.max(1, (int) data().setting("boon_every", 5));
    }

    // ---- Floors ----

    private void nextFloor(GauntletRun run) {
        if (run.over) return;
        run.floor++;
        run.state = State.STARTING;
        run.boss = null;
        run.bossPhase = 0;
        revive(run);
        GauntletData.Floor floor = data().floor(run.floor);
        boolean boss = floor != null && floor.boss();
        run.mutator = boss ? null : waves.rollMutator(run.floor);
        String subtitle = boss ? "<gold>A boss awaits"
                : run.mutator != null ? "<light_purple>" + run.mutator.name() + " <gray>- " + run.mutator.description()
                : "<gray>Get ready";
        Title title = Title.title(TextUtils.parse((boss ? "<dark_red><b>" : "<red><b>") + "FLOOR " + run.floor
                        + (run.heat > 0 ? " <gold>H" + run.heat : "")),
                TextUtils.parse(subtitle), Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(400)));
        for (Player player : run.online()) player.showTitle(title);
        if (run.mutator != null) run.tell("<light_purple><b>MUTATOR</b> <white>" + run.mutator.name()
                + " <gray>" + run.mutator.description() + " <aqua>(+" + run.mutator.shards() + "% Shards this floor)");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!run.over && !run.members.isEmpty()) startFight(run);
        }, FLOOR_DELAY_TICKS);
    }

    // The floor's wave comes in, and Bulwark and the Netherite Set give their absorption
    private void startFight(GauntletRun run) {
        waves.spawn(run);
        for (Player player : run.standing()) bonuses.floorAbsorption(player, run.members.get(player.getUniqueId()));
        run.state = State.FIGHTING;
    }

    // Everyone down is back on their feet at the arena's spawn, at half health: once the
    // floor is cleared, in time for the boons and the care package
    private void revive(GauntletRun run) {
        Location spawn = manager.arena().inSlot(run.slot, manager.arena().place(GauntletArena.SPAWN));
        for (Member member : run.members.values()) {
            Player player = Bukkit.getPlayer(member.id);
            if (member.down && player != null) standUp(member, player, spawn, 0.5);
        }
    }

    // A teammate sneaking where a player went down brings them back after revive_seconds,
    // where they fell, with revive_health percent of their health
    private void teammateRevives(GauntletRun run) {
        int needed = reviveSeconds();
        List<Player> standing = run.standing();
        for (Member member : run.members.values()) {
            Player downed = Bukkit.getPlayer(member.id);
            if (!member.down || member.fell == null || downed == null) continue;
            Player reviver = standing.stream().filter(mate -> mate.isSneaking() && mate.getWorld().equals(member.fell.getWorld())
                    && mate.getLocation().distanceSquared(member.fell) <= REVIVE_REACH * REVIVE_REACH).findFirst().orElse(null);
            if (reviver == null) {
                member.reviving = 0;
                member.reviver = null;
                continue;
            }
            member.reviver = reviver.getUniqueId();
            if (++member.reviving < needed) {
                reviver.playSound(reviver.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 0.8f + 0.3f * member.reviving);
                continue;
            }
            standUp(member, downed, member.fell, data().setting("revive_health", 30) / 100);
            manager.addStat(reviver.getUniqueId(), "revives", 1);
            run.tell("<green><b>REVIVED</b> <white>" + reviver.getName() + " <gray>brought <white>" + downed.getName() + " <gray>back");
        }
    }

    private int reviveSeconds() {
        return Math.max(1, (int) data().setting("revive_seconds", 3));
    }

    // A player who was down is on their feet again where they come back, with a share of
    // their most health
    private static void standUp(Member member, Player player, Location at, double share) {
        member.down = false;
        member.reviving = 0;
        member.reviver = null;
        clearMarker(member);
        player.setGameMode(member.mode);
        if (at != null) player.teleport(at);
        player.setHealth(Math.max(1, GauntletPlayers.maxHealth(player) * share));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.4f);
        TextUtils.send(player, "<green>You are back on your feet");
    }

    private static void clearMarker(Member member) {
        Entity marker = member.marker == null ? null : Bukkit.getEntity(member.marker);
        if (marker != null) marker.remove();
        member.marker = null;
    }

    // Every second: the wave hunts and the boss fights, the floor ends when no mob is
    // left, a choice nobody made is made, and the bars show how it is going
    private void tick() {
        for (GauntletRun run : new HashSet<>(runs.values())) {
            if (run.over || run.members.isEmpty()) continue;
            Location spawn = manager.arena().inSlot(run.slot, manager.arena().place(GauntletArena.SPAWN));
            if (run.state == State.FIGHTING) {
                waves.hunt(run, spawn);
                waves.bossPhases(run);
                waves.bossAbilities(run);
                teammateRevives(run);
                if (run.mobs.isEmpty()) cleared(run);
            } else if (run.state == State.CHOOSING
                    && System.currentTimeMillis() - run.choosingSince > data().setting("choice_seconds", 45) * 1000) {
                autoChoose(run);
            }
            if (run.over || run.members.isEmpty()) continue;
            updateBar(run);
            for (Player player : run.online()) {
                Member member = run.members.get(player.getUniqueId());
                keepInArena(run, player, member, spawn);
                manager.refreshBoard(player);
                player.sendActionBar(TextUtils.parse(status(run, member) + " <dark_gray>| <aqua>" + run.shards + " Shards"
                        + (run.cores > 0 ? " <dark_gray>| <light_purple>" + run.cores + " Cores" : "")));
            }
        }
    }

    // Fallen out of the arena, or down and watching from too far: back to its spawn
    private void keepInArena(GauntletRun run, Player player, Member member, Location spawn) {
        if (spawn == null) return;
        Location at = player.getLocation();
        boolean fell = !manager.arena().isInSlot(run.slot, at) && at.getY() < spawn.getY() - 30;
        boolean drifted = member.down && (!manager.arena().isOverSlot(run.slot, at) || Math.abs(at.getY() - spawn.getY()) > 30);
        if (fell || drifted) player.teleport(spawn);
    }

    // The action bar: the floor and mobs left with a combo going, a revive going on, or
    // the choice waiting
    private String status(GauntletRun run, Member member) {
        return switch (run.state) {
            case FIGHTING -> fightStatus(run, member);
            case CHOOSING -> member.choosing() ? (member.altar ? "<light_purple>Choose at the altar" : "<gold>Pick a boon")
                    + " <dark_gray>| <yellow><click:run_command:'/gauntlet boon'>/gauntlet boon</click>"
                    : "<gray>Waiting for the party to choose";
            case STARTING -> "<red>Floor " + run.floor;
        };
    }

    private String fightStatus(GauntletRun run, Member member) {
        int needed = reviveSeconds();
        if (member.down) {
            return member.reviving > 0 ? "<green>Being revived <white>" + member.reviving + "/" + needed
                    : "<red>You are down <gray>- a teammate can sneak where you fell, or clear the floor";
        }
        for (Member other : run.members.values()) {
            if (member.id.equals(other.reviver) && other.reviving > 0) {
                return "<yellow>Reviving <white>" + GauntletManager.nameOf(other.id) + " <gray>" + other.reviving + "/" + needed;
            }
        }
        return "<red>Floor " + run.floor + " <dark_gray>| <white>" + run.mobs.size() + " left"
                + (member.combo >= 2 && System.currentTimeMillis() - member.lastKill <= data().setting("combo_window", 3) * 1000
                ? " <dark_gray>| <gold><b>x" + member.combo + " COMBO</b>" : "");
    }

    // The boss's health on a boss floor, otherwise how much of the floor is cleared
    private void updateBar(GauntletRun run) {
        if (run.bar == null) return;
        LivingEntity boss = run.boss == null ? null : Bukkit.getEntity(run.boss) instanceof LivingEntity living ? living : null;
        if (run.state == State.FIGHTING && boss != null && boss.isValid()) {
            double max = CustomHealth.max(boss), now = CustomHealth.current(boss);
            MobData data = manager.registry().mobOf(boss);
            run.bar.name(TextUtils.parse("<dark_red><b>Floor " + run.floor + "</b> <dark_gray>» ").append(data == null
                    ? TextUtils.parse("<red>Boss") : data.displayName()).append(TextUtils.parse(" <red>" + Math.round(now) + "/" + Math.round(max))));
            run.bar.progress((float) Math.clamp(now / max, 0, 1));
            run.bar.color(BossBar.Color.PURPLE);
            return;
        }
        run.bar.color(BossBar.Color.RED);
        switch (run.state) {
            case FIGHTING -> {
                int total = Math.max(1, run.floorMobs);
                run.bar.name(TextUtils.parse("<red><b>Floor " + run.floor + "</b> <gray>" + (total - Math.min(total, run.mobs.size()))
                        + "/" + total + " killed" + (run.mutator != null ? " <light_purple>" + run.mutator.name() : "")
                        + (run.heat > 0 ? " <gold>Heat " + run.heat : "")));
                run.bar.progress((float) Math.clamp(1 - (double) run.mobs.size() / total, 0, 1));
            }
            case CHOOSING -> {
                boolean altar = run.members.values().stream().anyMatch(member -> member.altar);
                run.bar.name(TextUtils.parse(altar ? "<light_purple><b>An altar</b> <gray>choose to go on" : "<gold><b>Pick a boon</b> <gray>to go on"));
                run.bar.progress(1f);
            }
            case STARTING -> {
                run.bar.name(TextUtils.parse("<red><b>Floor " + run.floor + "</b> <gray>get ready"));
                run.bar.progress(0f);
            }
        }
    }

    // The floor is cleared: everyone down is back, the clear pays, then an altar, a care
    // package and boons, or straight on to the next floor
    private void cleared(GauntletRun run) {
        revive(run);
        boolean fast = payClear(run);
        records(run, fast);
        if (altarFloor(run.floor)) {
            openAltar(run);
        } else if (run.floor % boonEvery() == 0) {
            care.drop(run);
            offerBoons(run);
        } else {
            nextFloor(run);
        }
    }

    // The clear bonus and the floor's Cores, and under its par time a share of the bonus
    // again. Whether it was a fast clear
    private boolean payClear(GauntletRun run) {
        GauntletData.Floor floor = data().floor(run.floor);
        double mutated = (run.mutator == null ? 1 : 1 + run.mutator.shards() / 100.0) * heatPay(run);
        int bonus = (int) Math.round(floor.shards() * mutated);
        run.shards += bonus;
        run.cores += floor.cores();
        run.sound(Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
        run.tell("<green>Floor " + run.floor + " cleared <aqua>+" + bonus + " bonus Shards"
                + (floor.cores() > 0 ? " <light_purple>+" + floor.cores() + " Core" + (floor.cores() > 1 ? "s" : "") : ""));

        double seconds = (System.currentTimeMillis() - run.floorStarted) / 1000.0;
        double par = data().setting("par_seconds", 10) + data().setting("par_per_mob", 2.5) * run.floorMobs * (floor.boss() ? 2 : 1);
        if (seconds > par) return false;
        int extra = Math.max(1, (int) Math.round(bonus * data().setting("fast_bonus", 50) / 100));
        run.shards += extra;
        run.fastClears++;
        run.tell("<yellow><b>FAST CLEAR</b> <gray>" + Math.round(seconds) + "s (par " + Math.round(par) + "s) <aqua>+" + extra + " Shards");
        return true;
    }

    // Each player's daily challenges and NEW BEST once a run past their old record; a
    // tenth floor cleared for the first time is told to the whole Gauntlet
    private void records(GauntletRun run, boolean fast) {
        List<String> firsts = new ArrayList<>();
        for (Player player : run.online()) {
            Member member = run.members.get(player.getUniqueId());
            if (fast) manager.challenges().add(player, "fast_clears", 1);
            manager.challenges().best(player, "floor", run.floor);
            if (run.floor > member.bestBefore && run.floor % 10 == 0) firsts.add(player.getName());
            // Past their old record, once a run, unless it is their first run ever
            if (!member.newBest && run.floor > member.bestBefore && member.bestBefore > 0) {
                member.newBest = true;
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
                TextUtils.send(player, "<gold><b>NEW BEST!</b> <gray>Past your old record of floor " + member.bestBefore);
            }
        }
        sprint(run);
        if (!firsts.isEmpty()) announce("<gold>★ <white>" + names(firsts) + " <gray>cleared floor <gold>" + run.floor
                + "</gold> for the first time" + (run.heat > 0 ? " <gray>at <gold>heat " + run.heat : ""));
    }

    // A solo run from floor 1 that clears sprint_floor sets the player's fastest time
    private void sprint(GauntletRun run) {
        if (run.partySize != 1 || run.startFloor != 1 || run.floor != sprintFloor(data())) return;
        Player player = run.online().stream().findFirst().orElse(null);
        if (player == null) return;
        int seconds = (int) ((System.currentTimeMillis() - run.started) / 1000);
        int fastest = manager.profile(player).stat(SPRINT);
        if (fastest > 0 && seconds >= fastest) return;
        manager.setStat(player.getUniqueId(), SPRINT, Math.max(1, seconds));
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.2f);
        TextUtils.send(player, "<aqua><b>SPRINT</b> <gray>Floor " + run.floor + " in <white>" + clock(seconds) + "</white>, your fastest"
                + (fastest > 0 ? " <dark_gray>(was " + clock(fastest) + ")" : ""));
    }

    static int sprintFloor(GauntletData data) {
        return Math.max(1, (int) data.setting("sprint_floor", 10));
    }

    // 754 seconds as 12:34
    static String clock(long seconds) {
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    // To everyone in the Gauntlet's world, in a run or not
    private void announce(String message) {
        Location lobby = manager.arena().lobby();
        if (lobby == null || lobby.getWorld() == null) return;
        for (Player player : lobby.getWorld().getPlayers()) TextUtils.send(player, message);
    }

    // "A", "A and B", "A, B and C"
    static String names(List<String> names) {
        if (names.size() <= 1) return String.join("", names);
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.getLast();
    }

    // Everyone chooses a pact at the altar before the next floor
    private void openAltar(GauntletRun run) {
        run.state = State.CHOOSING;
        run.choosingSince = System.currentTimeMillis();
        run.sound(Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 0.7f);
        run.tell("<light_purple><b>AN ALTAR</b> <gray>A pact for the climb, or walk on: "
                + "<yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
        for (Player player : run.online()) {
            run.members.get(player.getUniqueId()).altar = true;
            new AltarMenu(manager, this).open(player);
        }
    }

    // An altar comes after altar_first, then every altar_every floors
    private boolean altarFloor(int floor) {
        int first = (int) data().setting("altar_first", 12), every = Math.max(1, (int) data().setting("altar_every", 20));
        return floor >= first && (floor - first) % every == 0;
    }

    // ---- Choices ----

    // A player's altar choice: the angel's heal and heart, the devil's epic boon for hearts, or nothing
    void altar(Player player, String choice) {
        GauntletRun run = runOf(player);
        Member member = memberOf(player);
        if (run == null || member == null || !member.altar) return;
        member.altar = false;
        player.closeInventory();
        pact(player, member, choice);
        choicesDone(run);
    }

    private void pact(Player player, Member member, String choice) {
        switch (choice) {
            case AltarMenu.ANGEL -> {
                member.altarHealth += 2 * data().setting("altar_angel_hearts", 1);
                bonuses.applyBoons(player, member);
                GauntletPlayers.heal(player);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
                TextUtils.send(player, "<yellow><b>ANGEL'S PACT</b> <gray>Healed, and a heart stronger");
            }
            case AltarMenu.DEVIL -> {
                List<String> epic = bonuses.pickBoons(member, 1, "epic");
                if (epic.isEmpty()) epic = bonuses.pickBoons(member, 1, "rare");
                member.altarHealth = Math.max(-10, member.altarHealth - 2 * data().setting("altar_devil_hearts", 2));
                if (!epic.isEmpty()) {
                    GauntletData.Boon boon = data().boons().get(epic.getFirst());
                    grant(member, boon);
                    TextUtils.send(player, "<dark_red><b>DEVIL'S PACT</b> <gray>For your hearts: " + boon.colour() + boon.name()
                            + " <gray>" + boon.description());
                }
                bonuses.applyBoons(player, member);
                GauntletPlayers.capHealth(player);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.6f);
            }
            default -> TextUtils.send(player, "<gray>You walk past the altar");
        }
    }

    // Once nobody has a boon or altar choice left, the next floor
    private void choicesDone(GauntletRun run) {
        if (run.over || run.state != State.CHOOSING || run.anyChoosing()) return;
        nextFloor(run);
    }

    // The choice timed out: the first boon offered, or walking past the altar, for
    // whoever has not chosen
    private void autoChoose(GauntletRun run) {
        for (Member member : run.members.values()) {
            if (!member.choosing()) continue;
            Player player = Bukkit.getPlayer(member.id);
            if (member.altar) {
                member.altar = false;
                if (player != null) {
                    player.closeInventory();
                    pact(player, member, AltarMenu.SKIP);
                }
            }
            if (!member.offered.isEmpty()) {
                String pick = member.offered.getFirst();
                member.offered = List.of();
                take(player, member, pick);
                if (player != null) {
                    player.closeInventory();
                    TextUtils.send(player, "<gray>Out of time, a boon was picked for you");
                }
            }
        }
        choicesDone(run);
    }

    // Every player's own pick of boons. The run is choosing before any menu opens, the
    // menu shows only what is offered while it is
    private void offerBoons(GauntletRun run) {
        List<Player> choosing = new ArrayList<>();
        for (Player player : run.online()) {
            Member member = run.members.get(player.getUniqueId());
            int choices = (int) data().setting("boon_choices", 3) + (int) bonuses.talentTotal(manager.profile(player), "insight");
            member.offered = List.copyOf(bonuses.pickBoons(member, choices, null));
            if (!member.offered.isEmpty()) choosing.add(player);
        }
        if (choosing.isEmpty()) {
            nextFloor(run);
            return;
        }
        run.state = State.CHOOSING;
        run.choosingSince = System.currentTimeMillis();
        for (Player player : choosing) {
            TextUtils.send(player, "<gold><b>BOON</b> <gray>Pick an upgrade for this run. Loot the care package first if you like: "
                    + "<yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
            new BoonMenu(manager.registry(), this, player).open(player);
        }
    }

    // For /gauntlet boon. Null if the menu opened, otherwise why not
    public String openBoons(Player player) {
        GauntletRun run = runOf(player);
        Member member = memberOf(player);
        if (run == null || member == null || run.state != State.CHOOSING || !member.choosing()) return "There is no boon to pick right now";
        if (member.altar) new AltarMenu(manager, this).open(player);
        else new BoonMenu(manager.registry(), this, player).open(player);
        return null;
    }

    List<GauntletData.Boon> offered(Player player) {
        GauntletRun run = runOf(player);
        Member member = memberOf(player);
        if (run == null || member == null || run.state != State.CHOOSING) return List.of();
        return member.offered.stream().map(id -> data().boons().get(id)).filter(Objects::nonNull).toList();
    }

    int stacks(Player player, String boon) {
        Member member = memberOf(player);
        return member == null ? 0 : member.stacks(boon);
    }

    int rerolls(Player player) {
        Member member = memberOf(player);
        return member == null ? 0 : member.rerolls;
    }

    // A fresh pick of boons, for a reroll. Null if it worked, otherwise why not
    String reroll(Player player) {
        GauntletRun run = runOf(player);
        Member member = memberOf(player);
        if (run == null || member == null || run.state != State.CHOOSING || member.offered.isEmpty()) return "There is no boon to pick right now";
        if (member.rerolls <= 0) return "No rerolls left this run";
        member.rerolls--;
        member.offered = List.copyOf(bonuses.pickBoons(member, member.offered.size(), null));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1f);
        return null;
    }

    void choose(Player player, String boonId) {
        GauntletRun run = runOf(player);
        Member member = memberOf(player);
        if (run == null || member == null || run.state != State.CHOOSING || !member.offered.contains(boonId)) return;
        member.offered = List.of();
        take(player, member, boonId);
        player.closeInventory();
        if (run.anyChoosing()) TextUtils.send(player, "<gray>Waiting for the rest of the party to choose");
        choicesDone(run);
    }

    // A boon a player takes, by choice or out of time
    private void take(Player player, Member member, String boonId) {
        GauntletData.Boon boon = data().boons().get(boonId);
        if (boon == null) return;
        grant(member, boon);
        if (player == null) return;
        bonuses.applyBoons(player, member);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        TextUtils.send(player, boon.colour() + boon.name() + " <gray>" + boon.description() + " <dark_gray>(x" + member.stacks(boonId) + ")");
        legendariesOpened(player, member, boon);
    }

    // The stack that opens a legendary up, said once: it is one of the next pick's boons
    private void legendariesOpened(Player player, Member member, GauntletData.Boon taken) {
        if (member.stacks(taken.id()) != Math.min((int) data().setting("legendary_stacks", 2), taken.max())) return;
        for (GauntletData.Boon legendary : data().boons().values()) {
            if (!legendary.requires().contains(taken.id()) || member.stacks(legendary.id()) > 0 || !bonuses.unlocked(member, legendary)) continue;
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.6f);
            TextUtils.send(player, "<gold><b>LEGENDARY</b> " + legendary.colour() + legendary.name()
                    + " <gray>has opened up, it is in your next pick of boons");
        }
    }

    // A boon into a player's run: one more stack, and Second Wind's revives at once
    private static void grant(Member member, GauntletData.Boon boon) {
        member.boons.merge(boon.id(), 1, Integer::sum);
        member.revives += (int) boon.effect("revive");
    }

    // A care package emptied and closed is gone. Closing the boon or altar menu without
    // choosing leaves the choice waiting, /gauntlet boon reopens it
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        GauntletRun run = runOf(player);
        Member member = memberOf(player);
        if (run == null || member == null) return;
        if (event.getInventory().getHolder(false) instanceof Chest chest) {
            if (event.getInventory().isEmpty()) care.closedEmpty(run, chest);
        } else if (event.getInventory().getHolder(false) instanceof AltarMenu && member.altar) {
            TextUtils.send(player, "<gray>The altar waits: <yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
        } else if (event.getInventory().getHolder(false) instanceof BoonMenu && !member.offered.isEmpty()) {
            TextUtils.send(player, "<gray>Your boon is waiting: <yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
        }
    }

    // ---- Deaths ----

    // A hit that would kill: a totem in hand or a Second Wind saves them; otherwise they
    // are down while anyone stands, and the run ends when nobody does
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethal(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inRun(player)) return;
        GauntletRun run = runOf(player);
        Member member = run.members.get(player.getUniqueId());
        if (member.down) {
            event.setCancelled(true);
            return;
        }
        if (event.getFinalDamage() < player.getHealth() + player.getAbsorptionAmount()) return;
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;
        event.setCancelled(true);
        if (member.revives > 0) {
            member.revives--;
            player.setHealth(GauntletPlayers.maxHealth(player) / 2);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.2);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.2f);
            TextUtils.send(player, "<gold><b>SECOND WIND</b> <gray>You cheat death" + (member.revives > 0 ? " (" + member.revives + " left)" : ""));
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.2f);
        if (run.standing().size() <= 1) {
            wipe(run);
            return;
        }
        down(run, member, player);
    }

    // Down until the floor is cleared or a teammate brings them back: watching, with a
    // marker where they fell (the arena's spawn if they fell off it)
    private void down(GauntletRun run, Member member, Player player) {
        Location spawn = manager.arena().inSlot(run.slot, manager.arena().place(GauntletArena.SPAWN));
        Location at = player.getLocation();
        boolean onArena = spawn == null
                || (manager.arena().isOverSlot(run.slot, at) && Math.abs(at.getY() - spawn.getY()) <= 3);
        member.down = true;
        member.mode = player.getGameMode();
        member.fell = onArena ? at : spawn;
        member.marker = member.fell.getWorld().spawn(member.fell.clone().add(0, 1.2, 0), TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(true);
            display.setPersistent(false);
            display.text(TextUtils.parse("<red><b>✚</b> <white>" + player.getName() + "<newline><yellow>Sneak here to revive"));
        }).getUniqueId();
        GauntletPlayers.heal(player);
        player.setGameMode(GameMode.SPECTATOR);
        run.tell("<red><b>DOWN</b> <white>" + player.getName() + " <gray>is down: sneak where they fell to bring them back, "
                + "or clear the floor");
    }

    // Every kill pays Shards into the party's pot at once, kept even if the run ends
    // before the floor is cleared: an elite elite_pay kills' worth, a boss five, a
    // mutated floor and a combo more
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        UUID id = mob.getUniqueId();
        for (GauntletRun run : new HashSet<>(runs.values())) {
            if (id.equals(run.hoarder)) {
                caughtHoarder(run);
                return;
            }
            if (!run.mobs.remove(id)) continue;
            run.kills++;
            Player killer = killerOf(run, mob);
            Member member = killer == null ? null : run.members.get(killer.getUniqueId());
            boolean elite = run.elites.remove(id);
            String mobId = mob.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
            String base = mobId == null ? null : GauntletData.baseMobId(mobId);
            if (member != null) credit(member, killer, elite, base);
            String affix = run.affixes.remove(id);
            if (affix != null) waves.affixDeath(run, mob, affix);
            if (id.equals(run.boss)) {
                run.boss = null;
                run.bossKills++;
                waves.bossGone(id);
                for (Player player : run.online()) manager.challenges().add(player, "boss_kills", 1);
            }
            payKill(run, base, killer, member, elite);
            if (member != null) killHeal(member, killer);
            return;
        }
    }

    // The player of the run who landed the kill. A mob dying with no killer of the run's
    // is the kill of a solo run's player, and of nobody's in a party
    private static Player killerOf(GauntletRun run, LivingEntity mob) {
        Player killer = mob.getKiller();
        if (killer != null && run.members.containsKey(killer.getUniqueId())) return killer;
        return run.members.size() == 1 ? Bukkit.getPlayer(run.members.keySet().iterator().next()) : null;
    }

    // The kill counts for whoever landed it: their kills, the bestiary's, their combo, their challenges
    private void credit(Member member, Player killer, boolean elite, String base) {
        member.kills++;
        if (elite) member.eliteKills++;
        if (base != null) member.slain.merge(base, 1, Integer::sum);
        long now = System.currentTimeMillis();
        member.combo = now - member.lastKill <= data().setting("combo_window", 3) * 1000 ? member.combo + 1 : 1;
        member.lastKill = now;
        member.bestCombo = Math.max(member.bestCombo, member.combo);
        manager.challenges().add(killer, "kills", 1);
        if (elite) manager.challenges().add(killer, "elite_kills", 1);
    }

    // What the kill pays into the pot. A party's floor pays what the same floor pays
    // alone, its extra mobs share it
    private void payKill(GauntletRun run, String base, Player killer, Member member, boolean elite) {
        GauntletData.Floor floor = data().floor(run.floor);
        if (base == null || floor == null) return;
        double pay = floor.killPay(base) * (elite ? data().setting("elite_pay", 3) : 1)
                * (run.mutator == null ? 1 : 1 + run.mutator.shards() / 100.0) * heatPay(run) * comboPay(member)
                / (floor.isBoss(base) ? 1 : run.floorScale);
        int paid = (int) Math.round(pay);
        run.shards += paid;
        if (killer != null) killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.6f);
        if (floor.isBoss(base)) run.tell("<gold>Boss slain <aqua>+" + paid + " Shards");
        else if (elite && killer != null) TextUtils.send(killer, "<gold>Elite slain <aqua>+" + paid + " Shards");
    }

    // Bloodthirst: half a heart a kill a stack, and the trident's Riptide
    private void killHeal(Member member, Player killer) {
        double heal = bonuses.boonTotal(member, "killheal") + bonuses.perkTotal(killer, "killheal");
        if (heal > 0) GauntletPlayers.heal(killer, heal);
    }

    // Every kill of a combo past the first pays combo_step percent more, up to combo_max
    private double comboPay(Member member) {
        if (member == null) return 1;
        int steps = Math.min(member.combo - 1, (int) data().setting("combo_max", 10));
        return 1 + Math.max(0, steps) * data().setting("combo_step", 5) / 100;
    }

    private void caughtHoarder(GauntletRun run) {
        run.hoarder = null;
        run.hoarders++;
        GauntletData.Floor floor = data().floor(run.floor);
        int paid = floor == null ? 0 : (int) Math.round(floor.kill() * data().setting("hoarder_pay", 20) * heatPay(run));
        run.shards += paid;
        run.sound(Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
        for (Player player : run.standing()) {
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.2);
        }
        run.tell("<gold><b>CAUGHT!</b> <gray>The Shard Hoarder drops <aqua>" + paid + " Shards");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer(), End.QUIT);
    }
}
