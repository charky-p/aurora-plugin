package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import org.charky.aurora.common.util.DataKeys;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;
import org.charky.aurora.features.combat.CombatHooks;
import org.charky.aurora.features.combat.CustomHealth;
import org.charky.aurora.features.combat.MobDamage;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.spawner.SpawnerManager;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// A run up the tower, solo, in the player's own copy of the arena. Every floor is a
// wave of mobs from floors.csv, made tougher by the floor; some are elites (tougher,
// they pay more) and from floor 6 a floor can carry a mutator (faster mobs, more of
// them ...) that pays more. Bosses call for help at two thirds and one third of their
// health and rage at the last third. Every kill pays Shards, clearing the floor a
// bonus (and more under its par time), boss floors Cores. Every boon_every floors a
// care package lands and the player picks a boon, upgrades for this run only, rarer
// ones less often. Talents bought in the lobby make every run stronger. A hit that
// would kill ends the run (unless a totem or a Second Wind saves them), and they are
// back in the lobby with what they earned, more with Greed and the day's first run.
// Veterans can start past the boss floors they have cleared (checkpoints)
public class GauntletRuns implements Listener {
    public enum End { DIED, LEFT, QUIT }

    private static final long FLOOR_DELAY_TICKS = 60L;
    // A mob this far from the arena's spawn has fallen off or wandered, it is brought back
    private static final double STRAY_DISTANCE = 80;
    private static final double CLEAVE_RANGE = 3;
    private static final double EXECUTE_BELOW = 0.3;
    private static final String DAILY_DAY = "daily_day";
    private static final String HEAT_CHOICE = "heat_choice";
    private static final String HOARDER = "g_hoarder";
    public static final String BEST_COMBO = "best_combo";
    private static final NamespacedKey HEALTH_BOON = new NamespacedKey("aurora", "gauntlet_health");
    private static final NamespacedKey SPEED_BOON = new NamespacedKey("aurora", "gauntlet_speed");
    private static final NamespacedKey ABSORB_BOON = new NamespacedKey("aurora", "gauntlet_absorb");
    private static final NamespacedKey TALENT_HEALTH = new NamespacedKey("aurora", "gauntlet_talent_health");
    private static final NamespacedKey ENRAGE = new NamespacedKey("aurora", "gauntlet_enrage");
    // Vanilla Strength does nothing to Aurora hits, in a run each level adds this much
    private static final double STRENGTH_PER_LEVEL = 0.3;

    private final Aurora plugin;
    private final GauntletManager manager;
    private final SpawnerManager spawners;
    private final Map<UUID, Run> runs = new HashMap<>();
    private final GauntletBossAbilities abilities;
    private BukkitTask task;

    private enum State { STARTING, FIGHTING, CHOOSING }

    private static final class Run {
        final UUID player;
        final int slot;
        final long started = System.currentTimeMillis();
        int startFloor = 1;
        int heat;
        int floor;
        int shards;
        int cores;
        State state = State.STARTING;
        final Set<UUID> mobs = new HashSet<>();
        final Set<UUID> elites = new HashSet<>();
        // Elites' affixes, by mob
        final Map<UUID, String> affixes = new HashMap<>();
        final Map<String, Integer> boons = new LinkedHashMap<>();
        List<String> offered = List.of();
        // The best floor before this run, and whether passing it was announced yet
        int bestBefore;
        boolean newBest;
        // This floor: its mutator, how many mobs it started with and when, its boss
        GauntletData.Mutator mutator;
        int floorMobs;
        long floorStarted;
        UUID boss;
        int bossPhase;
        // The run so far
        int kills;
        int eliteKills;
        int fastClears;
        int rerolls;
        int revives;
        boolean daily;
        BossBar bar;
        // Kills chained close together, and the run's longest chain
        long lastKill;
        int combo;
        int bestCombo;
        // A Shard Hoarder running loose, and how many were caught
        UUID hoarder;
        int hoarders;
        int bossKills;

        Run(UUID player, int slot) {
            this.player = player;
            this.slot = slot;
        }

        int stacks(String boon) {
            return boons.getOrDefault(boon, 0);
        }

        int cleared() {
            return state == State.FIGHTING || state == State.STARTING ? floor - 1 : floor;
        }
    }

    public GauntletRuns(Aurora plugin, GauntletManager manager, SpawnerManager spawners) {
        this.plugin = plugin;
        this.manager = manager;
        this.spawners = spawners;
        this.abilities = new GauntletBossAbilities(plugin, manager.registry());
        CombatHooks.set(this::outgoing, this::incoming, this::afterHit);
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Player player : plugin.getServer().getOnlinePlayers()) end(player, End.LEFT);
    }

    public boolean inRun(Player player) {
        return runs.containsKey(player.getUniqueId());
    }

    // For the sidebar, 0 outside a run
    int floorOf(Player player) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.floor;
    }

    int shardsOf(Player player) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.shards;
    }

    int coresOf(Player player) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.cores;
    }

    private GauntletData data() {
        return manager.data();
    }

    // ---- Starting and ending ----

    // The floors a run can start on: 1, and the floor after every boss floor cleared
    public List<Integer> checkpoints(Player player) {
        List<Integer> floors = new ArrayList<>(List.of(1));
        int best = manager.profile(player).best;
        for (int floor = 10; floor <= best; floor += 10) floors.add(floor + 1);
        return floors;
    }

    // From the menu, the start portal and /gauntlet start: straight into floor 1, or a
    // pick of checkpoints and heat for those past a boss. Null if it went on, otherwise why not
    public String startFlow(Player player) {
        String problem = canBegin(player);
        if (problem != null) return problem;
        if (checkpoints(player).size() > 1 || heatUnlocked(player) > 0) new CheckpointMenu(manager, this).open(player);
        else return begin(player, 1);
        return null;
    }

    // Heat makes a run harder and pay more; a level unlocks every few floors of the best
    public int heatUnlocked(Player player) {
        return data().heatUnlocked(manager.profile(player).best);
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

    private double heatPay(Run run) {
        return 1 + run.heat * data().setting("heat_shards", 30) / 100;
    }

    private String canBegin(Player player) {
        if (inRun(player)) return "You are already in a run";
        if (!manager.isHere(player)) return "Go to the Gauntlet first, /gauntlet";
        if (manager.pvp() != null && manager.pvp().inZone(player)) return "Leave the PvP zone first, /spawn";
        if (manager.inDuel(player)) return "Not during a duel";
        if (manager.isWatching(player)) return "Stop spectating first, /gauntlet spectate stop";
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
        String problem = canBegin(player);
        if (problem != null) return problem;
        if (!checkpoints(player).contains(startFloor)) return "You can't start on floor " + startFloor + " yet";

        GauntletProfile profile = manager.profile(player);
        GauntletArena arena = manager.arena();
        int slot = arena.claimSlot();
        Run run = new Run(player.getUniqueId(), slot);
        run.bestBefore = profile.best;
        run.startFloor = startFloor;
        run.heat = heat(player);
        run.floor = startFloor - 1;
        run.rerolls = (int) talentTotal(profile, "reroll");
        run.revives = (int) talentTotal(profile, "revive");
        long today = LocalDate.now().toEpochDay();
        run.daily = profile.stat(DAILY_DAY) != today;
        if (run.daily) manager.setStat(player.getUniqueId(), DAILY_DAY, (int) today);
        boolean first = profile.runs == 0;
        runs.put(player.getUniqueId(), run);
        arena.paste(slot);

        player.closeInventory();
        // The run starts with the setup from the lobby
        manager.giveKit(player);
        setModifier(player, Attribute.MAX_HEALTH, TALENT_HEALTH, talentTotal(profile, "health") + setTotal(profile, "health"),
                AttributeModifier.Operation.ADD_NUMBER);
        heal(player);
        player.teleport(arena.inSlot(slot, arena.place(GauntletArena.SPAWN)));
        player.playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 0.6f, 1f);
        run.bar = BossBar.bossBar(TextUtils.parse("<red><b>Floor " + startFloor), 0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        player.showBossBar(run.bar);

        if (run.heat > 0) TextUtils.send(player, "<gold><b>HEAT " + run.heat + "</b> <gray>Mobs have <white>+"
                + fmt(run.heat * data().setting("heat_health", 25)) + "%</white> health and hit <white>+"
                + fmt(run.heat * data().setting("heat_damage", 15)) + "%</white> harder, for <aqua>+"
                + fmt(run.heat * data().setting("heat_shards", 30)) + "% Shards");
        if (run.daily) TextUtils.send(player, "<gold><b>DAILY BONUS</b> <gray>Your first run today pays <white>"
                + fmt(data().setting("daily_bonus", 2)) + "x</white> Shards");
        if (first) {
            TextUtils.send(player, "<yellow><b>TIP</b> <gray>Kill every mob to clear a floor. Every "
                    + boonEvery() + " floors a care package lands and you pick a boon. If you fall, you keep the Shards you earned.");
        }
        // Starting past floor 1: the boons of the floors skipped, and a care package
        if (startFloor > 1) {
            List<String> given = new ArrayList<>();
            for (int i = 0; i < (startFloor - 1) / boonEvery(); i++) {
                List<String> pick = pickBoons(run, 1);
                if (pick.isEmpty()) break;
                run.boons.merge(pick.getFirst(), 1, Integer::sum);
                given.add(data().boons().get(pick.getFirst()).name());
            }
            applyBoons(player, run);
            if (!given.isEmpty()) TextUtils.send(player, "<gold><b>CHECKPOINT</b> <gray>Starting on floor " + startFloor
                    + " with: <white>" + String.join(", ", given));
            carePackage(player, run);
        }
        nextFloor(player, run);
        return null;
    }

    // Banks what the run earned and puts the player back in the lobby with their setup
    public void end(Player player, End why) {
        Run run = runs.remove(player.getUniqueId());
        if (run == null) return;
        for (UUID id : run.mobs) {
            Entity mob = Bukkit.getEntity(id);
            if (mob != null) mob.remove();
        }
        if (run.bar != null) player.hideBossBar(run.bar);
        if (run.hoarder != null && Bukkit.getEntity(run.hoarder) != null) Bukkit.getEntity(run.hoarder).remove();
        manager.arena().freeSlot(run.slot);
        clearBoons(player);
        takeRunItems(player);

        GauntletProfile profile = manager.profile(player);
        int cleared = run.cleared();
        boolean record = cleared > run.bestBefore;
        double greed = boonTotal(run, "shards") + talentTotal(profile, "shards");
        double daily = run.daily ? data().setting("daily_bonus", 2) : 1;
        int banked = (int) Math.round(run.shards * (1 + greed / 100) * daily);
        manager.bank(player, banked, run.cores, cleared);
        manager.addStat(player.getUniqueId(), "kills", run.kills);
        manager.addStat(player.getUniqueId(), "elite_kills", run.eliteKills);
        manager.addStat(player.getUniqueId(), "boss_kills", run.bossKills);
        manager.addStat(player.getUniqueId(), "hoarders", run.hoarders);
        manager.addStat(player.getUniqueId(), "fast_clears", run.fastClears);
        if (run.bestCombo > profile.stat(BEST_COMBO)) manager.setStat(player.getUniqueId(), BEST_COMBO, run.bestCombo);
        // A run counts for the challenge once it cleared a floor
        if (cleared >= run.startFloor) manager.challenges().add(player, "runs", 1);
        if (why == End.QUIT) return;

        heal(player);
        if (manager.isHere(player)) {
            Location lobby = manager.arena().lobby();
            if (lobby != null) player.teleport(lobby);
            manager.giveKit(player);
        }
        player.showTitle(Title.title(TextUtils.parse(why == End.DIED ? "<red><b>DEFEATED" : "<gray><b>RUN OVER"),
                TextUtils.parse(record ? "<gold><b>NEW BEST</b> <gray>Floors cleared: <white>" + cleared
                        : "<gray>Floors cleared: <white>" + cleared)));
        summary(player, run, why, cleared, record, greed, daily, banked);
        if (profile.runs == 1) {
            TextUtils.send(player, "<yellow><b>TIP</b> <gray>Spend your Shards on gear in the forge, every level hits harder: "
                    + "<click:run_command:'/gauntlet forge'><gold><b>[FORGE]</b></click>");
        }
    }

    private void summary(Player player, Run run, End why, int cleared, boolean record, double greed, double daily, int banked) {
        long seconds = (System.currentTimeMillis() - run.started) / 1000;
        String time = seconds / 60 + ":" + String.format("%02d", seconds % 60);
        TextUtils.send(player, "");
        TextUtils.send(player, "<gradient:#FF416C:#FF4B2B><b>RUN OVER</b></gradient> <gray>" + (why == End.DIED
                ? "fell on floor <white>" + run.floor : "left on floor <white>" + run.floor)
                + (run.startFloor > 1 ? " <dark_gray>(started on " + run.startFloor + ")" : "")
                + (run.heat > 0 ? " <gold>Heat " + run.heat : ""));
        TextUtils.send(player, "<gray>Cleared <white>" + cleared + "</white> in <white>" + time + "</white>, <white>"
                + run.kills + "</white> kills (" + run.eliteKills + " elite), <white>" + run.fastClears + "</white> fast clears, best combo <gold>x"
                + run.bestCombo + (run.hoarders > 0 ? "<gray>, <gold>" + run.hoarders + "</gold> hoarder" + (run.hoarders > 1 ? "s" : "") + " caught" : ""));
        String bonus = (greed > 0 ? " <gray>+" + fmt(greed) + "% greed" : "") + (daily > 1 ? " <gray>x" + fmt(daily) + " daily" : "");
        TextUtils.send(player, "<gray>Shards: <aqua>" + run.shards + bonus + (bonus.isEmpty() ? "" : " <gray>= <aqua><b>" + banked + "</b>")
                + (run.cores > 0 ? " <gray>Cores: <light_purple>" + run.cores : ""));
        if (!run.boons.isEmpty()) {
            List<String> boons = new ArrayList<>();
            run.boons.forEach((id, stacks) -> {
                GauntletData.Boon boon = data().boons().get(id);
                if (boon != null) boons.add(boon.colour() + boon.name() + (stacks > 1 ? " x" + stacks : ""));
            });
            TextUtils.send(player, "<gray>Boons: " + String.join("<gray>, ", boons));
        }
        TextUtils.send(player, record ? "<gold><b>NEW BEST!</b> <gray>(was " + run.bestBefore + ")"
                : "<gray>Best: <white>" + manager.profile(player).best);
        TextUtils.send(player, "");
    }

    private static String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private void heal(Player player) {
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(max == null ? 20 : max.getValue());
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setFireTicks(0);
    }

    private int boonEvery() {
        return Math.max(1, (int) data().setting("boon_every", 5));
    }

    // ---- Floors ----

    private void nextFloor(Player player, Run run) {
        run.floor++;
        run.state = State.STARTING;
        run.boss = null;
        run.bossPhase = 0;
        GauntletData.Floor floor = data().floor(run.floor);
        boolean boss = floor != null && floor.boss();
        run.mutator = boss ? null : rollMutator(run.floor);
        String subtitle = boss ? "<gold>A boss awaits"
                : run.mutator != null ? "<light_purple>" + run.mutator.name() + " <gray>- " + run.mutator.description()
                : "<gray>Get ready";
        player.showTitle(Title.title(TextUtils.parse((boss ? "<dark_red><b>" : "<red><b>") + "FLOOR " + run.floor
                        + (run.heat > 0 ? " <gold>H" + run.heat : "")),
                TextUtils.parse(subtitle), Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(400))));
        if (run.mutator != null) TextUtils.send(player, "<light_purple><b>MUTATOR</b> <white>" + run.mutator.name()
                + " <gray>" + run.mutator.description() + " <aqua>(+" + run.mutator.shards() + "% Shards this floor)");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (runs.get(player.getUniqueId()) == run && player.isOnline()) spawnFloor(player, run);
        }, FLOOR_DELAY_TICKS);
    }

    private GauntletData.Mutator rollMutator(int floor) {
        if (data().mutators().isEmpty() || floor < data().setting("mutator_from", 6)) return null;
        if (ThreadLocalRandom.current().nextDouble(100) >= data().setting("mutator_chance", 25)) return null;
        List<GauntletData.Mutator> all = List.copyOf(data().mutators().values());
        return all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }

    // Percent chance a mob of this floor is elite
    private double eliteChance(Run run) {
        GauntletData data = data();
        double from = data.setting("elite_from", 3);
        if (run.floor < from) return 0;
        double chance = Math.min(data.setting("elite_chance_max", 15),
                data.setting("elite_chance", 4) + data.setting("elite_chance_per_floor", 0.2) * (run.floor - from));
        return chance * (run.mutator == null ? 1 : run.mutator.elites());
    }

    private void spawnFloor(Player player, Run run) {
        GauntletData.Floor floor = data().floor(run.floor);
        GauntletArena arena = manager.arena();
        List<Location> spawns = new ArrayList<>(arena.mobSpawns().stream().map(spawn -> arena.inSlot(run.slot, spawn)).toList());
        Collections.shuffle(spawns);
        double eliteChance = eliteChance(run);
        String mutatorId = run.mutator == null ? null : run.mutator.id();
        int next = 0;
        int elites = 0;
        for (Map.Entry<String, Integer> entry : floor.mobs().entrySet()) {
            boolean boss = floor.isBoss(entry.getKey());
            int count = boss ? entry.getValue() : (int) Math.ceil(entry.getValue() * (run.mutator == null ? 1 : run.mutator.count()));
            for (int i = 0; i < count; i++) {
                boolean elite = !boss && ThreadLocalRandom.current().nextDouble(100) < eliteChance;
                String affix = elite ? rollAffix(run) : null;
                String id = new GauntletData.Variant(entry.getKey(), run.floor, elite, affix, run.heat, mutatorId).id();
                Location at = spawns.get(next++ % spawns.size()).clone()
                        .add(ThreadLocalRandom.current().nextDouble(-1.5, 1.5), 0, ThreadLocalRandom.current().nextDouble(-1.5, 1.5));
                LivingEntity mob = spawn(player, run, id, at);
                if (mob == null) continue;
                if (boss) run.boss = mob.getUniqueId();
                if (elite) {
                    run.elites.add(mob.getUniqueId());
                    elites++;
                }
                if (affix != null) {
                    run.affixes.put(mob.getUniqueId(), affix);
                    if (affix.equals("swift")) {
                        AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
                        if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.4);
                    }
                }
            }
        }
        run.floorMobs = run.mobs.size();
        run.floorStarted = System.currentTimeMillis();
        if (!floor.boss() && run.floor >= data().setting("hoarder_from", 3)
                && ThreadLocalRandom.current().nextDouble(100) < data().setting("hoarder_chance", 8)) {
            hoarder(player, run, spawns.get(ThreadLocalRandom.current().nextInt(spawns.size())));
        }
        if (elites > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.4f);
            player.sendActionBar(TextUtils.parse("<gold><b>" + elites + " elite" + (elites > 1 ? "s" : "") + "</b> <gray>on this floor"));
        }
        // Bulwark: absorption hearts at the start of every floor
        int absorb = (int) Math.round(boonTotal(run, "absorb") + setTotal(manager.profile(player), "absorb"));
        if (absorb > 0) {
            setModifier(player, Attribute.MAX_ABSORPTION, ABSORB_BOON, absorb, AttributeModifier.Operation.ADD_NUMBER);
            player.setAbsorptionAmount(absorb);
        }
        run.state = State.FIGHTING;
    }

    // A Shard Hoarder: quick, glowing, harmless, not needed to clear the floor, and worth
    // hoarder_pay kills if caught before it gets away
    private void hoarder(Player player, Run run, Location at) {
        if (manager.registry().getMob(HOARDER) == null) return;
        LivingEntity hoarder = spawners.spawnGauntletMob(new GauntletData.Variant(HOARDER, run.floor, false, run.heat, null).id(), at);
        if (hoarder == null) return;
        run.hoarder = hoarder.getUniqueId();
        int seconds = (int) data().setting("hoarder_seconds", 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 2f);
        TextUtils.send(player, "<gold><b>SHARD HOARDER</b> <gray>appeared! Catch it in <white>" + seconds + " seconds</white> for a pile of Shards");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!hoarder.isValid() || hoarder.isDead()) return;
            hoarder.getWorld().spawnParticle(Particle.POOF, hoarder.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
            hoarder.remove();
            if (runs.get(run.player) == run && hoarder.getUniqueId().equals(run.hoarder)) {
                run.hoarder = null;
                if (player.isOnline()) TextUtils.send(player, "<gray>The Shard Hoarder got away");
            }
        }, seconds * 20L);
    }

    // An affix for an elite from affix_from on, null before or without any
    private String rollAffix(Run run) {
        if (data().affixes().isEmpty() || run.floor < data().setting("affix_from", 10)) return null;
        List<String> ids = List.copyOf(data().affixes().keySet());
        return ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
    }

    // One mob of the run, hunting the player, sped up by the floor's mutator
    private LivingEntity spawn(Player player, Run run, String id, Location at) {
        LivingEntity mob = spawners.spawnGauntletMob(id, at);
        if (mob == null) return null;
        run.mobs.add(mob.getUniqueId());
        if (run.mutator != null && run.mutator.speed() != 1) {
            AttributeInstance speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(speed.getBaseValue() * run.mutator.speed());
        }
        if (mob instanceof Mob hunter) hunter.setTarget(player);
        return mob;
    }

    // Every second: mobs that are gone count as dead, strays are brought back, a boss
    // calls for help as it weakens, the floor ends when none are left, and the boss
    // bar and action bar show how it is going
    private void tick() {
        for (Run run : new ArrayList<>(runs.values())) {
            Player player = Bukkit.getPlayer(run.player);
            if (player == null) continue;
            if (run.state == State.FIGHTING) {
                Location spawn = manager.arena().inSlot(run.slot, manager.arena().place(GauntletArena.SPAWN));
                run.mobs.removeIf(id -> {
                    Entity mob = Bukkit.getEntity(id);
                    if (mob == null || !mob.isValid() || mob.isDead()) return true;
                    if (spawn != null && (!mob.getWorld().equals(spawn.getWorld()) || mob.getLocation().distance(spawn) > STRAY_DISTANCE)) {
                        mob.teleport(spawn);
                    }
                    if (mob instanceof Mob hunter && hunter.getTarget() == null) hunter.setTarget(player);
                    return false;
                });
                bossPhases(player, run);
                bossAbilities(player, run);
                if (run.mobs.isEmpty()) cleared(player, run);
                // The player fell out of their arena
                else if (spawn != null && !manager.arena().isInSlot(run.slot, player.getLocation())
                        && player.getLocation().getY() < spawn.getY() - 30) {
                    player.teleport(spawn);
                }
            }
            if (!runs.containsKey(run.player)) continue;
            updateBar(run);
            String status = switch (run.state) {
                case FIGHTING -> "<red>Floor " + run.floor + " <dark_gray>| <white>" + run.mobs.size() + " left"
                        + (run.combo >= 2 && System.currentTimeMillis() - run.lastKill <= data().setting("combo_window", 3) * 1000
                        ? " <dark_gray>| <gold><b>x" + run.combo + " COMBO</b>" : "");
                case CHOOSING -> "<gold>Pick a boon <dark_gray>| <yellow><click:run_command:'/gauntlet boon'>/gauntlet boon</click>";
                case STARTING -> "<red>Floor " + run.floor;
            };
            manager.refreshBoard(player);
            player.sendActionBar(TextUtils.parse(status + " <dark_gray>| <aqua>" + run.shards + " Shards"
                    + (run.cores > 0 ? " <dark_gray>| <light_purple>" + run.cores + " Cores" : "")));
        }
    }

    // The boss's health on a boss floor, otherwise how much of the floor is cleared
    private void updateBar(Run run) {
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
                run.bar.name(TextUtils.parse("<gold><b>Pick a boon</b> <gray>to go on"));
                run.bar.progress(1f);
            }
            case STARTING -> {
                run.bar.name(TextUtils.parse("<red><b>Floor " + run.floor + "</b> <gray>get ready"));
                run.bar.progress(0f);
            }
        }
    }

    // At two thirds and one third of its health a boss calls the floor's other mobs to
    // its side; at the last third it rages, moving faster
    private void bossPhases(Player player, Run run) {
        if (run.boss == null || run.bossPhase >= 2 || !(Bukkit.getEntity(run.boss) instanceof LivingEntity boss) || !boss.isValid()) return;
        double left = CustomHealth.current(boss) / CustomHealth.max(boss);
        while (run.bossPhase < 2 && left <= (2 - run.bossPhase) / 3.0) {
            run.bossPhase++;
            callAdds(player, run, boss);
            if (run.bossPhase == 2) {
                AttributeInstance speed = boss.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.removeModifier(ENRAGE);
                    speed.addModifier(new AttributeModifier(ENRAGE, data().setting("boss_enrage_speed", 20) / 100,
                            AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
                }
                boss.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, boss.getLocation().add(0, boss.getHeight(), 0), 10, 0.5, 0.3, 0.5, 0);
                player.playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.8f);
                TextUtils.send(player, "<dark_red><b>ENRAGED</b> <gray>The boss moves faster now");
            }
        }
    }

    // The boss's own abilities (bosses.csv), fought in this run
    private void bossAbilities(Player player, Run run) {
        if (run.boss == null || !(Bukkit.getEntity(run.boss) instanceof LivingEntity boss) || !boss.isValid()) return;
        String mobId = boss.getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
        List<GauntletData.BossAbility> list = data().bosses().getOrDefault(GauntletData.baseMobId(mobId), List.of());
        abilities.tick(player, boss, list, run.floor, new GauntletBossAbilities.Fight() {
            @Override
            public boolean on() {
                return runs.get(run.player) == run && run.state == State.FIGHTING;
            }

            @Override
            public void summon(int count) {
                callAdds(player, run, boss, count);
            }

            @Override
            public double incoming(Player victim, double damage) {
                return GauntletRuns.this.incoming(victim, boss, damage);
            }
        });
    }

    private void callAdds(Player player, Run run, LivingEntity boss) {
        callAdds(player, run, boss, (int) data().setting("boss_adds", 2) + run.floor / 10);
    }

    private void callAdds(Player player, Run run, LivingEntity boss, int adds) {
        if (spawnAdds(player, run, boss.getLocation(), adds) == 0) return;
        player.playSound(boss.getLocation(), Sound.EVENT_RAID_HORN, 0.5f, 1.4f);
        TextUtils.send(player, "<dark_red><b>REINFORCEMENTS</b> <gray>The boss calls <white>" + adds + "</white> to its side");
    }

    // The floor's other mobs, plain ones, around a spot. How many came
    private int spawnAdds(Player player, Run run, Location around, int adds) {
        GauntletData.Floor floor = data().floor(run.floor);
        List<String> kinds = floor.mobs().keySet().stream().filter(id -> !floor.isBoss(id)).toList();
        if (kinds.isEmpty() || adds <= 0) return 0;
        String mutatorId = run.mutator == null ? null : run.mutator.id();
        for (int i = 0; i < adds; i++) {
            String kind = kinds.get(ThreadLocalRandom.current().nextInt(kinds.size()));
            Location at = around.clone().add(ThreadLocalRandom.current().nextDouble(-2, 2), 0, ThreadLocalRandom.current().nextDouble(-2, 2));
            spawn(player, run, new GauntletData.Variant(kind, run.floor, false, run.heat, mutatorId).id(), at);
        }
        run.floorMobs += adds;
        return adds;
    }

    private void cleared(Player player, Run run) {
        GauntletData.Floor floor = data().floor(run.floor);
        double mutated = (run.mutator == null ? 1 : 1 + run.mutator.shards() / 100.0) * heatPay(run);
        int bonus = (int) Math.round(floor.shards() * mutated);
        run.shards += bonus;
        run.cores += floor.cores();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
        TextUtils.send(player, "<green>Floor " + run.floor + " cleared <aqua>+" + bonus + " bonus Shards"
                + (floor.cores() > 0 ? " <light_purple>+" + floor.cores() + " Core" + (floor.cores() > 1 ? "s" : "") : ""));

        // Under its par time: a share of the clear bonus again
        double seconds = (System.currentTimeMillis() - run.floorStarted) / 1000.0;
        double par = data().setting("par_seconds", 10) + data().setting("par_per_mob", 2.5) * run.floorMobs * (floor.boss() ? 2 : 1);
        if (seconds <= par) {
            int fast = Math.max(1, (int) Math.round(bonus * data().setting("fast_bonus", 50) / 100));
            run.shards += fast;
            run.fastClears++;
            manager.challenges().add(player, "fast_clears", 1);
            TextUtils.send(player, "<yellow><b>FAST CLEAR</b> <gray>" + Math.round(seconds) + "s (par " + Math.round(par) + "s) <aqua>+" + fast + " Shards");
        }

        manager.challenges().best(player, "floor", run.floor);

        // Past the old record, once a run, unless it is the first one ever
        if (!run.newBest && run.floor > run.bestBefore && run.bestBefore > 0) {
            run.newBest = true;
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
            TextUtils.send(player, "<gold><b>NEW BEST!</b> <gray>Past your old record of floor " + run.bestBefore);
        }

        if (run.floor % boonEvery() == 0) {
            carePackage(player, run);
            offerBoons(player, run);
        } else {
            nextFloor(player, run);
        }
    }

    // A hit that would kill ends the run, unless a totem in hand or a Second Wind saves them
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethal(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !inRun(player)) return;
        if (event.getFinalDamage() < player.getHealth() + player.getAbsorptionAmount()) return;
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;
        event.setCancelled(true);
        Run run = runs.get(player.getUniqueId());
        if (run.revives > 0) {
            run.revives--;
            AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
            player.setHealth((max == null ? 20 : max.getValue()) / 2);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.2);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.2f);
            TextUtils.send(player, "<gold><b>SECOND WIND</b> <gray>You cheat death" + (run.revives > 0 ? " (" + run.revives + " left)" : ""));
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.2f);
        end(player, End.DIED);
    }

    // Every kill pays Shards at once, kept even if the run ends before the floor is
    // cleared: an elite elite_pay kills' worth, a boss five, a mutated floor more
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        for (Run run : runs.values()) {
            if (id.equals(run.hoarder)) {
                caughtHoarder(run);
                return;
            }
            if (!run.mobs.remove(id)) continue;
            Player player = Bukkit.getPlayer(run.player);
            run.kills++;
            long now = System.currentTimeMillis();
            run.combo = now - run.lastKill <= data().setting("combo_window", 3) * 1000 ? run.combo + 1 : 1;
            run.lastKill = now;
            run.bestCombo = Math.max(run.bestCombo, run.combo);
            boolean elite = run.elites.remove(id);
            if (elite) run.eliteKills++;
            String affix = run.affixes.remove(id);
            if (affix != null && player != null) afterAffixDeath(player, run, event.getEntity(), affix);
            boolean boss = id.equals(run.boss);
            if (boss) {
                run.boss = null;
                run.bossKills++;
                abilities.forget(id);
            }
            if (player != null) {
                GauntletChallenges challenges = manager.challenges();
                challenges.add(player, "kills", 1);
                if (elite) challenges.add(player, "elite_kills", 1);
                if (boss) challenges.add(player, "boss_kills", 1);
            }
            String mobId = event.getEntity().getPersistentDataContainer().get(DataKeys.MOB_KEY, PersistentDataType.STRING);
            GauntletData.Floor floor = data().floor(run.floor);
            if (mobId != null && floor != null) {
                String base = GauntletData.baseMobId(mobId);
                double pay = floor.killPay(base) * (elite ? data().setting("elite_pay", 3) : 1)
                        * (run.mutator == null ? 1 : 1 + run.mutator.shards() / 100.0) * heatPay(run) * comboPay(run);
                int paid = (int) Math.round(pay);
                run.shards += paid;
                if (player != null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.6f);
                    if (floor.isBoss(base)) TextUtils.send(player, "<gold>Boss slain <aqua>+" + paid + " Shards");
                    else if (elite) TextUtils.send(player, "<gold>Elite slain <aqua>+" + paid + " Shards");
                }
            }
            // Bloodthirst: half a heart a kill a stack, and the trident's Riptide
            double heal = boonTotal(run, "killheal") + (player == null ? 0 : perkTotal(player, "killheal"));
            if (player != null && heal > 0) {
                AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
                player.setHealth(Math.min(max == null ? 20 : max.getValue(), player.getHealth() + heal));
            }
            return;
        }
    }

    // Explosive goes off a second after it dies; Summoner calls two of the floor's mobs
    private void afterAffixDeath(Player player, Run run, LivingEntity dead, String affix) {
        Location at = dead.getLocation();
        if (affix.equals("summoner")) {
            if (spawnAdds(player, run, at, 2) > 0) {
                at.getWorld().spawnParticle(Particle.SOUL, at.clone().add(0, 1, 0), 25, 0.6, 0.6, 0.6, 0.02);
                player.playSound(at, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 1.2f);
            }
            return;
        }
        if (!affix.equals("explosive")) return;
        MobData mob = manager.registry().mobOf(dead);
        at.getWorld().spawnParticle(Particle.SMOKE, at.clone().add(0, 0.5, 0), 30, 0.4, 0.3, 0.4, 0.02);
        at.getWorld().playSound(at, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (runs.get(run.player) != run || !player.isOnline()) return;
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 3, 0.5, 0.3, 0.5, 0);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.1f);
            if (!player.getWorld().equals(at.getWorld()) || player.getLocation().distance(at) > 3.5) return;
            double damage = mob == null ? 4 : MobDamage.dealt(mob, player, false) * 1.5;
            damage = incoming(player, null, damage);
            if (damage > 0) player.damage(damage);
        }, 20L);
    }

    // Every kill of a combo past the first pays combo_step percent more, up to combo_max
    private double comboPay(Run run) {
        int steps = Math.min(run.combo - 1, (int) data().setting("combo_max", 10));
        return 1 + Math.max(0, steps) * data().setting("combo_step", 5) / 100;
    }

    private void caughtHoarder(Run run) {
        run.hoarder = null;
        run.hoarders++;
        GauntletData.Floor floor = data().floor(run.floor);
        int paid = floor == null ? 0 : (int) Math.round(floor.kill() * data().setting("hoarder_pay", 20) * heatPay(run));
        run.shards += paid;
        Player player = Bukkit.getPlayer(run.player);
        if (player == null) return;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.2);
        TextUtils.send(player, "<gold><b>CAUGHT!</b> <gray>The Shard Hoarder drops <aqua>" + paid + " Shards");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        end(event.getPlayer(), End.QUIT);
    }

    // ---- Care packages ----

    // A chest by the arena's package spot (or the player) with the care package's loot,
    // every item a little likelier with Scavenger. All of it is taken back when the run ends
    private void carePackage(Player player, Run run) {
        GauntletArena arena = manager.arena();
        Location at = arena.place(GauntletArena.PACKAGE);
        at = at == null ? player.getLocation() : arena.inSlot(run.slot, at);
        Block block = at.getBlock();
        block.setType(Material.CHEST);
        if (!(block.getState() instanceof Chest chest)) return;

        GauntletProfile profile = manager.profile(player);
        GauntletData.CareLevel care = data().careLevel(profile.care);
        double scavenger = talentTotal(profile, "care");
        if (care != null) {
            for (GauntletData.Loot loot : care.loot()) {
                if (ThreadLocalRandom.current().nextDouble(100) >= loot.chance() + scavenger) continue;
                ItemStack item = lootItem(loot.item(), ThreadLocalRandom.current().nextInt(loot.min(), loot.max() + 1));
                if (item == null) continue;
                GauntletGear.markRunItem(item);
                chest.getBlockInventory().addItem(item);
            }
        }
        block.getWorld().spawnParticle(Particle.FIREWORK, block.getLocation().add(0.5, 1, 0.5), 40, 0.4, 0.6, 0.4, 0.05);
        player.playSound(block.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
        TextUtils.send(player, "<gold><b>CARE PACKAGE</b> <gray>A chest has landed in the arena (level " + profile.care + ")");
    }

    // A material, or POTION@TYPE / SPLASH_POTION@TYPE. Null for anything unknown
    static ItemStack lootItem(String id, int amount) {
        String[] parts = id.split("@", 2);
        Material material = Material.matchMaterial(parts[0]);
        if (material == null || !material.isItem() || amount <= 0) return null;
        ItemStack item = new ItemStack(material, amount);
        if (parts.length == 2 && item.getItemMeta() instanceof PotionMeta) {
            PotionType type;
            try {
                type = PotionType.valueOf(parts[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
            item.editMeta(PotionMeta.class, meta -> meta.setBasePotionType(type));
        }
        return item;
    }

    private void takeRunItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (GauntletGear.isRunItem(inventory.getItem(slot))) inventory.setItem(slot, null);
        }
        ItemStack cursor = player.getItemOnCursor();
        if (GauntletGear.isRunItem(cursor)) player.setItemOnCursor(null);
    }

    // ---- Boons ----

    // Boons not taken to their max, picked by the weight of their rarity, none twice
    private List<String> pickBoons(Run run, int count) {
        List<GauntletData.Boon> open = new ArrayList<>();
        for (GauntletData.Boon boon : data().boons().values()) {
            if (run.stacks(boon.id()) < boon.max()) open.add(boon);
        }
        List<String> picked = new ArrayList<>();
        while (picked.size() < count && !open.isEmpty()) {
            double total = 0;
            for (GauntletData.Boon boon : open) total += rarityWeight(boon);
            double roll = ThreadLocalRandom.current().nextDouble(total);
            GauntletData.Boon chosen = open.getLast();
            for (GauntletData.Boon boon : open) {
                roll -= rarityWeight(boon);
                if (roll < 0) {
                    chosen = boon;
                    break;
                }
            }
            picked.add(chosen.id());
            open.remove(chosen);
        }
        return picked;
    }

    private double rarityWeight(GauntletData.Boon boon) {
        return Math.max(0.01, data().setting("rarity_" + boon.rarity(), boon.rarity().equals("common") ? 60 : 30));
    }

    private void offerBoons(Player player, Run run) {
        int choices = (int) data().setting("boon_choices", 3) + (int) talentTotal(manager.profile(player), "insight");
        run.offered = List.copyOf(pickBoons(run, choices));
        if (run.offered.isEmpty()) {
            nextFloor(player, run);
            return;
        }
        run.state = State.CHOOSING;
        TextUtils.send(player, "<gold><b>BOON</b> <gray>Pick an upgrade for this run. Loot the care package first if you like: "
                + "<yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
        new BoonMenu(manager.registry(), this, player).open(player);
    }

    // For /gauntlet boon. Null if the menu opened, otherwise why not
    public String openBoons(Player player) {
        Run run = runs.get(player.getUniqueId());
        if (run == null || run.state != State.CHOOSING) return "There is no boon to pick right now";
        new BoonMenu(manager.registry(), this, player).open(player);
        return null;
    }

    List<GauntletData.Boon> offered(Player player) {
        Run run = runs.get(player.getUniqueId());
        if (run == null || run.state != State.CHOOSING) return List.of();
        return run.offered.stream().map(id -> data().boons().get(id)).filter(Objects::nonNull).toList();
    }

    int stacks(Player player, String boon) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.stacks(boon);
    }

    int rerolls(Player player) {
        Run run = runs.get(player.getUniqueId());
        return run == null ? 0 : run.rerolls;
    }

    // A fresh pick of boons, for a reroll. Null if it worked, otherwise why not
    String reroll(Player player) {
        Run run = runs.get(player.getUniqueId());
        if (run == null || run.state != State.CHOOSING) return "There is no boon to pick right now";
        if (run.rerolls <= 0) return "No rerolls left this run";
        run.rerolls--;
        run.offered = List.copyOf(pickBoons(run, run.offered.size()));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1f);
        return null;
    }

    void choose(Player player, String boonId) {
        Run run = runs.get(player.getUniqueId());
        if (run == null || run.state != State.CHOOSING || !run.offered.contains(boonId)) return;
        GauntletData.Boon boon = data().boons().get(boonId);
        run.boons.merge(boonId, 1, Integer::sum);
        run.offered = List.of();
        if (boon.stat().equals("revive")) run.revives += (int) boon.amount();
        applyBoons(player, run);
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
        TextUtils.send(player, boon.colour() + boon.name() + " <gray>" + boon.description() + " <dark_gray>(x" + run.stacks(boonId) + ")");
        nextFloor(player, run);
    }

    // A care package emptied and closed is gone. Closing the boon menu without picking
    // leaves the choice waiting, /gauntlet boon reopens it
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof Chest chest && event.getInventory().isEmpty()
                && event.getPlayer() instanceof Player owner) {
            Run run = runs.get(owner.getUniqueId());
            Block block = chest.getBlock();
            if (run != null && manager.arena().isInSlot(run.slot, block.getLocation())) {
                block.setType(Material.AIR);
                block.getWorld().spawnParticle(Particle.POOF, block.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.02);
                owner.playSound(block.getLocation(), Sound.BLOCK_WOOL_BREAK, 0.8f, 1f);
            }
            return;
        }
        if (!(event.getInventory().getHolder(false) instanceof BoonMenu)) return;
        if (event.getPlayer() instanceof Player player && !offered(player).isEmpty()) {
            TextUtils.send(player, "<gray>Your boon is waiting: <yellow><click:run_command:'/gauntlet boon'>[Choose]</click>");
        }
    }

    // A boon's stat added up over its stacks
    private double boonTotal(Run run, String stat) {
        double total = 0;
        for (Map.Entry<String, Integer> entry : run.boons.entrySet()) {
            GauntletData.Boon boon = data().boons().get(entry.getKey());
            if (boon != null && boon.stat().equals(stat)) total += boon.amount() * entry.getValue();
        }
        return total;
    }

    // A perk stat of the gear held (or worn as a set), for the levels the player has
    private double perkTotal(Player player, String stat) {
        GauntletProfile profile = manager.profile(player);
        String held = GauntletGear.gearId(player.getInventory().getItemInMainHand());
        double total = 0;
        if (held != null) {
            for (GauntletData.Perk perk : data().perksOf(held)) {
                if (perk.stat().equals(stat) && profile.level(held) >= perk.level()) total += perk.amount();
            }
        }
        return total + setTotal(profile, stat);
    }

    // An armour set stat: every set perk whose level all four pieces have reached
    private double setTotal(GauntletProfile profile, String stat) {
        int lowest = armorSetLevel(profile);
        double total = 0;
        for (GauntletData.Perk perk : data().perksOf(GauntletData.ARMOR_SET)) {
            if (perk.stat().equals(stat) && lowest >= perk.level()) total += perk.amount();
        }
        return total;
    }

    // The lowest level of the armour pieces, 0 while one is missing
    int armorSetLevel(GauntletProfile profile) {
        int lowest = Integer.MAX_VALUE;
        boolean any = false;
        for (GauntletData.Gear gear : data().gear().values()) {
            if (!gear.armor()) continue;
            any = true;
            lowest = Math.min(lowest, Math.max(0, profile.level(gear.id())));
        }
        return any ? lowest : 0;
    }

    // A talent's stat added up over its levels
    private double talentTotal(GauntletProfile profile, String stat) {
        double total = 0;
        for (GauntletData.Talent talent : data().talents().values()) {
            if (talent.stat().equals(stat)) total += talent.amount() * profile.talent(talent.id());
        }
        return total;
    }

    private void applyBoons(Player player, Run run) {
        setModifier(player, Attribute.MAX_HEALTH, HEALTH_BOON, boonTotal(run, "health"), AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.MOVEMENT_SPEED, SPEED_BOON, boonTotal(run, "speed") / 100, AttributeModifier.Operation.ADD_SCALAR);
        int regen = (int) Math.round(boonTotal(run, "regen"));
        player.removePotionEffect(PotionEffectType.REGENERATION);
        if (regen > 0) player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, regen - 1, true, false));
    }

    private void clearBoons(Player player) {
        setModifier(player, Attribute.MAX_HEALTH, HEALTH_BOON, 0, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.MAX_HEALTH, TALENT_HEALTH, 0, AttributeModifier.Operation.ADD_NUMBER);
        setModifier(player, Attribute.MOVEMENT_SPEED, SPEED_BOON, 0, AttributeModifier.Operation.ADD_SCALAR);
        setModifier(player, Attribute.MAX_ABSORPTION, ABSORB_BOON, 0, AttributeModifier.Operation.ADD_NUMBER);
        player.setAbsorptionAmount(0);
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
    }

    private static void setModifier(Player player, Attribute attribute, NamespacedKey key, double amount,
                                    AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(key);
        if (amount != 0) instance.addModifier(new AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY));
    }

    // ---- Combat ----

    // Might, First Strike, Executioner and Giant Slayer add up; Keen Eye doubles; then
    // Strength potions and the weapon's own share, in a run only
    private double outgoing(Player attacker, LivingEntity victim, boolean ranged) {
        Run run = runs.get(attacker.getUniqueId());
        if (run == null) return 1.0;
        double percent = boonTotal(run, "damage") + perkTotal(attacker, "damage");
        double health = CustomHealth.current(victim) / Math.max(1, CustomHealth.max(victim));
        if (health >= 0.999) percent += boonTotal(run, "firststrike") + perkTotal(attacker, "firststrike");
        if (health < EXECUTE_BELOW) percent += boonTotal(run, "execute") + perkTotal(attacker, "execute");
        if (victim.getUniqueId().equals(run.boss) || run.elites.contains(victim.getUniqueId())) {
            percent += boonTotal(run, "slayer") + perkTotal(attacker, "slayer");
        }
        double multiplier = 1 + percent / 100;
        if ("shielded".equals(run.affixes.get(victim.getUniqueId())) && health >= 0.5) multiplier *= 0.5;
        if (ThreadLocalRandom.current().nextDouble(100) < boonTotal(run, "crit") + perkTotal(attacker, "crit")) multiplier *= 2;
        PotionEffect strength = attacker.getPotionEffect(PotionEffectType.STRENGTH);
        if (strength != null) multiplier *= 1 + STRENGTH_PER_LEVEL * (strength.getAmplifier() + 1);
        if (!ranged) {
            GauntletData.Gear gear = data().gear().get(GauntletGear.gearId(attacker.getInventory().getItemInMainHand()));
            if (gear != null) multiplier *= gear.damage() / 100.0;
        }
        return multiplier;
    }

    // Cleave: a share of every melee hit to the run's other mobs close to the one struck
    private void afterHit(Player attacker, LivingEntity victim, double dealt, boolean ranged) {
        Run run = runs.get(attacker.getUniqueId());
        if (run == null || ranged || dealt <= 0 || !run.mobs.contains(victim.getUniqueId())) return;
        double cleave = boonTotal(run, "cleave") + perkTotal(attacker, "cleave");
        if (cleave <= 0) return;
        double splash = Math.round(dealt * cleave / 100);
        if (splash <= 0) return;
        for (Entity near : victim.getNearbyEntities(CLEAVE_RANGE, CLEAVE_RANGE, CLEAVE_RANGE)) {
            if (near instanceof LivingEntity other && run.mobs.contains(other.getUniqueId()) && !other.isDead()) {
                CustomHealth.apply(other, splash, attacker, manager.registry());
                other.getWorld().spawnParticle(Particle.SWEEP_ATTACK, other.getLocation().add(0, 1, 0), 1);
            }
        }
    }

    // Evasion and Iron Skin, and the attacking elite's affix: Vampiric heals, Frenzied
    // hits harder below half health
    private double incoming(Player victim, LivingEntity source, double damage) {
        Run run = runs.get(victim.getUniqueId());
        if (run == null) return damage;
        String affix = source == null ? null : run.affixes.get(source.getUniqueId());
        if ("frenzied".equals(affix) && CustomHealth.current(source) < CustomHealth.max(source) / 2) damage *= 1.3;
        if ("vampiric".equals(affix)) {
            CustomHealth.heal(source, CustomHealth.max(source) * 0.1, manager.registry());
            source.getWorld().spawnParticle(Particle.HEART, source.getLocation().add(0, source.getHeight(), 0), 2, 0.3, 0.2, 0.3, 0);
        }
        if (ThreadLocalRandom.current().nextDouble(100) < boonTotal(run, "dodge")) {
            victim.playSound(victim.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.6f, 1.6f);
            return 0;
        }
        return damage * (1 - Math.min(0.8, (boonTotal(run, "guard") + setTotal(manager.profile(victim), "guard")) / 100));
    }
}
