package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.spawner.MobData;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

// gauntlet.yml, from scripts/data/gauntlet/ through generate.py: the floors, the gear
// and what each level of it costs, the boons, the care package levels and the
// milestones. Floor and gear numbers come from scripts/balance/gauntlet_balance.py
public record GauntletData(List<Floor> floors, Map<String, Gear> gear, List<GearLevel> levels,
                           Map<String, Boon> boons, List<CareLevel> care, List<Milestone> milestones,
                           Map<String, SetupItem> setup, Map<String, Talent> talents, Map<String, Mutator> mutators,
                           Map<String, Challenge> challenges, Map<String, List<BossAbility>> bosses,
                           List<Perk> perks, Map<String, Affix> affixes, List<Achievement> achievements,
                           List<ShopItem> shop, Map<String, Double> settings,
                           List<Theme> themes) {
    // Without themes, for data put together by hand
    public GauntletData(List<Floor> floors, Map<String, Gear> gear, List<GearLevel> levels, Map<String, Boon> boons, List<CareLevel> care,
                        List<Milestone> milestones, Map<String, SetupItem> setup, Map<String, Talent> talents, Map<String, Mutator> mutators,
                        Map<String, Challenge> challenges, Map<String, List<BossAbility>> bosses, List<Perk> perks, Map<String, Affix> affixes,
                        List<Achievement> achievements, List<ShopItem> shop, Map<String, Double> settings) {
        this(floors, gear, levels, boons, care, milestones, setup, talents, mutators, challenges, bosses, perks, affixes, achievements,
                shop, settings, List.of());
    }

    // The gear a perk belongs to when it is for the whole armour set
    public static final String ARMOR_SET = "armor_set";
    private static final String FILE = "gauntlet.yml";
    // Past the last floor in the file it repeats, every mob this much tougher a floor
    // and ATTACK_PER_FLOOR attack levels stronger (gauntlet_balance.py)
    static final double ENDLESS_HEALTH = 1.08;
    static final double ATTACK_PER_FLOOR = 2.2;
    // A mob on a floor is "<mob id>#<floor>", made from its base on the fly
    static final String FLOOR_SEP = "#";
    // A boss kill pays this many kills (gauntlet_balance.py)
    static final int BOSS_KILLS = 5;
    // Past the last floor a kill pays this much more a floor
    static final double KILL_PER_FLOOR = 0.08;

    // kill is what every mob killed pays, shards the bonus for clearing the floor
    public record Floor(Map<String, Integer> mobs, double health, int attack, int kill, int shards, int cores) {
        public boolean boss() {
            return cores > 0;
        }

        // A boss floor's boss is its one mob of a kind, the Rot King among four Rotters
        public boolean isBoss(String mobId) {
            return boss() && mobs.getOrDefault(mobId, 0) == 1;
        }

        // What killing one of these pays
        public int killPay(String mobId) {
            return isBoss(mobId) ? kill * BOSS_KILLS : kill;
        }
    }

    // startLevel: the level it is unlocked at, so a piece bought later is not left weak
    public record Gear(String id, String name, String item, List<String> materials, String kind, int damage,
                       int unlockShards, int unlockFloor, int startLevel) {
        public boolean starter() {
            return unlockShards <= 0 && unlockFloor <= 0;
        }

        public boolean armor() {
            return kind.equals("armor");
        }
    }

    public record GearLevel(int level, int shards, int cores, int material, int melee, int ranged, int defence,
                            int protection, int deflection) {
    }

    // A boon: what a stack of it adds (one stat or several), its most stacks and rarity,
    // and for a legendary the boons it needs before it can be offered
    public record Boon(String id, String name, String icon, String description, Map<String, Double> effects, int max,
                       String rarity, List<String> requires) {
        public double effect(String stat) {
            return effects.getOrDefault(stat, 0.0);
        }

        public boolean legendary() {
            return !requires.isEmpty();
        }

        public String colour() {
            return switch (rarity) {
                case "legendary" -> "<gold>";
                case "epic" -> "<light_purple>";
                case "rare" -> "<aqua>";
                default -> "<white>";
            };
        }
    }

    // A permanent upgrade for runs, bought with Shards (and Cores) a level at a time.
    // The first level costs shards, each one after growth times the last
    public record Talent(String id, String name, String icon, String description, String stat, double amount, int max,
                         int shards, double growth, int cores) {
        public int shardsFor(int level) {
            return (int) Math.round(shards * Math.pow(growth, level - 1));
        }

        public int coresFor(int level) {
            return cores * level;
        }
    }

    // A daily challenge: reach goal of a stat counted over the day (floor is the best
    // floor cleared in one run). With scale the goal rises to the player's best floor
    public record Challenge(String id, String description, String stat, int goal, int shards, int cores, boolean scale) {
        public int goalFor(int best) {
            return scale ? Math.max(goal, best / 5 * 5) : goal;
        }

        public String describe(int best) {
            return description.replace("{goal}", String.valueOf(goalFor(best)));
        }
    }

    // What a piece of gear unlocks at a level, in runs only: a weapon's while it is held,
    // the armour set's with every piece at the level (perks.csv)
    public record Perk(String gear, int level, String name, String description, String stat, double amount) {
    }

    public List<Perk> perksOf(String gearId) {
        return perks.stream().filter(perk -> perk.gear().equals(gearId)).toList();
    }

    // A cosmetic sold for Shards in the Shard Shop (shardshop.csv)
    public record ShopItem(String cosmetic, int shards) {
    }

    // What an elite can roll from affix_from on (affixes.csv), shown in its name
    public record Affix(String id, String name, String colour, String description) {
    }

    // A long goal (achievements.csv): reach goal of a stat for Essence, Shards and a cosmetic
    public record Achievement(String id, String name, String description, String stat, int goal, int essence,
                              int shards, String cosmetic) {
        public String describe() {
            return description.replace("{goal}", String.format("%,d", goal));
        }
    }

    // One of a boss's abilities (see GauntletBossAbilities) and how often, in seconds
    public record BossAbility(String type, int cooldown) {
    }

    // What can change a floor: its mobs' health, hits, speed, how many there are, how
    // often they are elite, and the percent more Shards the floor pays
    // A floor's twist: its mobs' health, damage, speed and count, how often they are elite,
    // the percent more Shards it pays, and an affix every mob but an elite carries ("" for none)
    public record Mutator(String id, String name, String description, double health, double damage, double speed,
                          double count, double elites, int shards, String affix) {
    }

    public record Loot(String item, int min, int max, double chance) {
    }

    public record CareLevel(int level, int shards, int cores, List<Loot> loot) {
    }

    public record Milestone(int floor, int essence, String cosmetic) {
    }

    // A starting kit item, bought a level at a time for good, handed out at the start of
    // every run. Level n costs n times shards and cores. The item is vanilla's (as care
    // package loot), amount of it a level, or the main game's: TOOL@grapple, amount levels
    // of each of its upgrades for every level after the first, or ITEM@dash_feather
    public record SetupItem(String id, String name, String item, int amount, int max, int shards, int cores,
                            String description) {
        private static final String TOOL = "TOOL@";
        private static final String ITEM = "ITEM@";

        public int shardsFor(int level) {
            return shards * level;
        }

        public int coresFor(int level) {
            return cores * level;
        }

        // The main game's tool it is, null for anything else
        public String tool() {
            return item.startsWith(TOOL) ? item.substring(TOOL.length()) : null;
        }

        // The main game's item it is, null for anything else
        public String mainItem() {
            return item.startsWith(ITEM) ? item.substring(ITEM.length()) : null;
        }

        // How many a level starts a run with, at most a stack
        public int count(int level) {
            return Math.min(amount * level, 64);
        }

        // The level of each of a main-game tool's upgrades, none at the first level
        public int upgrades(int level) {
            return amount * Math.max(0, level - 1);
        }

        // What a level starts a run with, as the menu and chat say it
        public String startsWith(int level) {
            return tool() == null ? String.valueOf(count(level)) : level == 0 ? "nothing" : "level " + level;
        }
    }

    // A band of floors' theme, from its first floor on: its name, colour and a line about it
    public record Theme(int floor, String name, String colour, String description) {
        public String title() {
            return "<" + colour + ">" + name;
        }
    }

    // The theme of a floor: the last to start at or below it. Null without any
    public Theme theme(int floor) {
        Theme found = null;
        for (Theme theme : themes) if (theme.floor() <= floor) found = theme;
        return found;
    }

    // The first floor of the next theme after a floor's, or past the tower's last floor
    public int themeEnd(int floor) {
        for (Theme theme : themes) if (theme.floor() > floor) return theme.floor() - 1;
        return Math.max(floor, floors.size());
    }

    public static GauntletData empty() {
        return new GauntletData(List.of(), Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of());
    }

    // Empty when the file is missing or broken, the gauntlet then refuses to start
    public static GauntletData load(JavaPlugin plugin, Collection<String> knownMobs) {
        File file = new File(plugin.getDataFolder(), FILE);
        if (!file.exists()) {
            try {
                plugin.saveResource(FILE, false);
            } catch (IllegalArgumentException e) {
                return empty();
            }
        }
        try (InputStream in = new FileInputStream(file)) {
            Map<String, Object> root = new Yaml().load(in);
            return root == null ? empty() : parse(root, knownMobs, plugin.getLogger());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load " + FILE, e);
            return empty();
        }
    }

    // gauntlet.yml as generate.py writes it. A floor's mobs that don't exist are left out and logged
    @SuppressWarnings("unchecked")
    static GauntletData parse(Map<String, Object> root, Collection<String> knownMobs, Logger logger) {
        List<Floor> floors = rows(root, "floors", row -> {
            Map<String, Integer> mobs = new LinkedHashMap<>();
            ((Map<String, Object>) row.getOrDefault("mobs", Map.of())).forEach((mob, count) -> {
                if (knownMobs.contains(mob)) mobs.put(mob, num(count).intValue());
                else logger.severe("GAUNTLET ERROR: no mob '" + mob + "'");
            });
            return new Floor(mobs, num(row.get("health")).doubleValue(), num(row.get("attack")).intValue(),
                    Math.max(1, num(row.get("kill")).intValue()), num(row.get("shards")).intValue(), num(row.get("cores")).intValue());
        });
        Map<String, Gear> gear = byId(root, "gear", (id, row) -> new Gear(id, str(row.get("name")), str(row.get("item")),
                ((List<Object>) row.getOrDefault("materials", List.of())).stream().map(String::valueOf).toList(), str(row.get("kind")),
                num(row.get("damage")).intValue(), num(row.get("unlock_shards")).intValue(), num(row.get("unlock_floor")).intValue(),
                num(row.get("start_level")).intValue()));
        List<GearLevel> levels = rows(root, "levels", row -> new GearLevel(num(row.get("level")).intValue(),
                num(row.get("shards")).intValue(), num(row.get("cores")).intValue(), num(row.get("material")).intValue(),
                num(row.get("melee")).intValue(), num(row.get("ranged")).intValue(), num(row.get("defence")).intValue(),
                num(row.get("protection")).intValue(), num(row.get("deflection")).intValue()));
        Map<String, Boon> boons = byId(root, "boons", (id, row) -> new Boon(id, str(row.get("name")), str(row.get("icon")),
                str(row.get("description")), effects(row), num(row.get("max")).intValue(),
                row.get("rarity") == null ? "common" : str(row.get("rarity")),
                ((List<Object>) row.getOrDefault("requires", List.of())).stream().map(String::valueOf).toList()));
        List<CareLevel> care = rows(root, "care", row -> new CareLevel(num(row.get("level")).intValue(),
                num(row.get("shards")).intValue(), num(row.get("cores")).intValue(),
                rows(row, "loot", loot -> new Loot(str(loot.get("item")), num(loot.get("min")).intValue(),
                        num(loot.get("max")).intValue(), num(loot.get("chance")).doubleValue()))));
        List<Milestone> milestones = rows(root, "milestones", row -> new Milestone(num(row.get("floor")).intValue(),
                num(row.get("essence")).intValue(), str(row.get("cosmetic"))));
        Map<String, SetupItem> setup = byId(root, "setup", (id, row) -> new SetupItem(id, str(row.get("name")), str(row.get("item")),
                num(row.get("amount")).intValue(), num(row.get("max")).intValue(), num(row.get("shards")).intValue(),
                num(row.get("cores")).intValue(), str(row.get("description"))));
        Map<String, Talent> talents = byId(root, "talents", (id, row) -> new Talent(id, str(row.get("name")), str(row.get("icon")),
                str(row.get("description")), str(row.get("stat")), num(row.get("amount")).doubleValue(), num(row.get("max")).intValue(),
                num(row.get("shards")).intValue(), num(row.get("growth")).doubleValue(), num(row.get("cores")).intValue()));
        Map<String, Mutator> mutators = byId(root, "mutators", (id, row) -> new Mutator(id, str(row.get("name")),
                str(row.get("description")), num(row.get("health")).doubleValue(), num(row.get("damage")).doubleValue(),
                num(row.get("speed")).doubleValue(), num(row.get("count")).doubleValue(), num(row.get("elites")).doubleValue(),
                num(row.get("shards")).intValue(), str(row.get("affix"))));
        Map<String, Challenge> challenges = byId(root, "challenges", (id, row) -> new Challenge(id, str(row.get("description")),
                str(row.get("stat")), num(row.get("goal")).intValue(), num(row.get("shards")).intValue(), num(row.get("cores")).intValue(),
                Boolean.TRUE.equals(row.get("scale"))));
        Map<String, List<BossAbility>> bosses = new LinkedHashMap<>();
        ((Map<String, Object>) root.getOrDefault("bosses", Map.of())).forEach((id, value) -> bosses.put(id, list(value).stream()
                .map(ability -> new BossAbility(str(ability.get("type")), Math.max(1, num(ability.get("cooldown")).intValue()))).toList()));
        List<Perk> perks = rows(root, "perks", row -> new Perk(str(row.get("gear")), num(row.get("level")).intValue(),
                str(row.get("name")), str(row.get("description")), str(row.get("stat")), num(row.get("amount")).doubleValue()));
        Map<String, Affix> affixes = byId(root, "affixes", (id, row) -> new Affix(id, str(row.get("name")), str(row.get("colour")),
                str(row.get("description"))));
        List<Achievement> achievements = rows(root, "achievements", row -> new Achievement(str(row.get("id")), str(row.get("name")),
                str(row.get("description")), str(row.get("stat")), num(row.get("goal")).intValue(), num(row.get("essence")).intValue(),
                num(row.get("shards")).intValue(), str(row.get("cosmetic"))));
        List<ShopItem> shop = rows(root, "shop", row -> new ShopItem(str(row.get("cosmetic")), num(row.get("shards")).intValue()));
        Map<String, Double> settings = new LinkedHashMap<>();
        ((Map<String, Object>) root.getOrDefault("settings", Map.of())).forEach((key, value) -> settings.put(key, num(value).doubleValue()));
        List<Theme> themes = rows(root, "themes", row -> new Theme(num(row.get("floor")).intValue(), str(row.get("name")),
                str(row.get("colour")), str(row.get("description"))));
        return new GauntletData(floors, gear, levels, boons, care, milestones, setup, talents, mutators, challenges, bosses, perks,
                affixes, achievements, shop, settings, themes);
    }

    // A boon's stats and what a stack adds to each; a file from before effects has one
    private static Map<String, Double> effects(Map<String, Object> row) {
        Map<String, Double> effects = new LinkedHashMap<>();
        if (row.get("effects") instanceof Map<?, ?> listed) {
            listed.forEach((stat, amount) -> effects.put(String.valueOf(stat), num(amount).doubleValue()));
        } else {
            effects.put(str(row.get("stat")), num(row.get("amount")).doubleValue());
        }
        return effects;
    }

    // A section that is a list, one record a row, in order
    private static <T> List<T> rows(Map<String, Object> parent, String key, Function<Map<String, Object>, T> read) {
        return list(parent.get(key)).stream().map(read).toList();
    }

    // A section keyed by id, one record a row, in the file's order
    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> byId(Map<String, Object> root, String key, BiFunction<String, Map<String, Object>, T> read) {
        Map<String, T> records = new LinkedHashMap<>();
        ((Map<String, Map<String, Object>>) root.getOrDefault(key, Map.of())).forEach((id, row) -> records.put(id, read.apply(id, row)));
        return records;
    }

    // A tuning number from settings.csv, or the fallback when it is missing
    public double setting(String key, double fallback) {
        return settings.getOrDefault(key, fallback);
    }

    // The highest heat a best floor unlocks: heat_first for 1, heat_every more for each after
    public int heatUnlocked(int best) {
        double first = setting("heat_first", 15), every = Math.max(1, setting("heat_every", 10));
        if (best < first) return 0;
        return (int) Math.min(setting("heat_max", 5), 1 + Math.floor((best - first) / every));
    }

    public boolean ready() {
        return !floors.isEmpty() && !levels.isEmpty() && !gear.isEmpty();
    }

    // Floors count from 1. Past the last one it repeats, harder each floor
    public Floor floor(int number) {
        if (floors.isEmpty()) return null;
        if (number <= floors.size()) return floors.get(number - 1);
        Floor last = floors.getLast();
        int past = number - floors.size();
        return new Floor(last.mobs(), last.health() * Math.pow(ENDLESS_HEALTH, past),
                (int) Math.round(last.attack() + ATTACK_PER_FLOOR * past),
                (int) Math.round(last.kill() + KILL_PER_FLOOR * past), last.shards() + past,
                number % 10 == 0 ? Math.max(1, last.cores()) : 0);
    }

    public int maxLevel() {
        return levels.isEmpty() ? 0 : levels.getLast().level();
    }

    // The highest level gear is upgraded to before the next boss falls: level_cap_per_boss
    // for each boss beaten and as many again, so a sword waits at 30 for floor 30's boss
    public int levelCap(int best) {
        return (int) Math.round((best / 10 + 1) * setting("level_cap_per_boss", 10));
    }

    // The boss floor that raises the level cap next, past the best floor cleared
    public static int nextBoss(int best) {
        return (best / 10 + 1) * 10;
    }

    // Null past the top
    public GearLevel level(int level) {
        return level >= 0 && level < levels.size() ? levels.get(level) : null;
    }

    public CareLevel careLevel(int level) {
        for (CareLevel care : this.care) {
            if (care.level() == level) return care;
        }
        return care.isEmpty() ? null : care.getFirst();
    }

    public int maxCareLevel() {
        return care.isEmpty() ? 0 : care.getLast().level();
    }

    // A mob as it is on a floor: g_rotter#12, g_rotter#12* when elite, g_rotter#12*^2
    // at heat 2, and g_rotter#12*^2~swift with the floor's mutator. Made from the base
    // mob on the fly
    public record Variant(String base, int floor, boolean elite, String affix, int heat, String mutator) {
        static final String ELITE = "*";
        static final String AFFIX = "!";
        static final String HEAT = "^";
        static final String MUTATOR = "~";

        public Variant(String base, int floor, boolean elite, int heat, String mutator) {
            this(base, floor, elite, null, heat, mutator);
        }

        public String id() {
            return base + FLOOR_SEP + floor + (elite ? ELITE : "") + (affix == null ? "" : AFFIX + affix)
                    + (heat > 0 ? HEAT + heat : "") + (mutator == null ? "" : MUTATOR + mutator);
        }

        // Null for an id that is no variant
        public static Variant parse(String id) {
            if (id == null) return null;
            String mutator = null;
            int tilde = id.indexOf(MUTATOR);
            if (tilde >= 0) {
                mutator = id.substring(tilde + 1);
                id = id.substring(0, tilde);
            }
            try {
                int heat = 0;
                int caret = id.indexOf(HEAT);
                if (caret >= 0) {
                    heat = Integer.parseInt(id.substring(caret + 1));
                    id = id.substring(0, caret);
                }
                String affix = null;
                int bang = id.indexOf(AFFIX);
                if (bang >= 0) {
                    affix = id.substring(bang + 1);
                    id = id.substring(0, bang);
                }
                boolean elite = id.endsWith(ELITE);
                if (elite) id = id.substring(0, id.length() - 1);
                int sep = id.lastIndexOf(FLOOR_SEP);
                if (sep < 0) return null;
                return new Variant(id.substring(0, sep), Integer.parseInt(id.substring(sep + 1)), elite, affix, heat, mutator);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // The base mob of a floor mob id, g_rotter from g_rotter#12*~swift
    public static String baseMobId(String variantId) {
        Variant variant = Variant.parse(variantId);
        return variant == null ? variantId : variant.base();
    }

    // A mob on its floor: the base mob's health times the floor's, its attack plus the
    // floor's; an elite's health and hits times elite_health and elite_damage, the
    // heat's on top (heat_health and heat_damage a level), then the floor's mutator.
    // Null for an id that is not one
    public MobData variant(String id, Function<String, MobData> base) {
        Variant variant = Variant.parse(id);
        if (variant == null) return null;
        MobData mob = base.apply(variant.base());
        Floor floor = floor(variant.floor());
        if (mob == null || floor == null) return null;
        Mutator mutator = variant.mutator() == null ? null : mutators.get(variant.mutator());
        double health = mob.health() * floor.health() * (variant.elite() ? setting("elite_health", 2.5) : 1)
                * (1 + variant.heat() * setting("heat_health", 25) / 100) * (mutator == null ? 1 : mutator.health());
        double damage = mob.damage() * (variant.elite() ? setting("elite_damage", 1.3) : 1)
                * (1 + variant.heat() * setting("heat_damage", 15) / 100) * (mutator == null ? 1 : mutator.damage());
        Affix affix = variant.affix() == null ? null : affixes.get(variant.affix());
        Component name = (variant.elite() ? TextUtils.parse("<gold><b>Elite</b> ") : Component.empty())
                .append(affix == null ? Component.empty() : TextUtils.parse("<" + affix.colour() + "><b>" + affix.name() + "</b> "))
                .append(mob.displayName()).append(TextUtils.parse(" <dark_gray>F" + variant.floor()));
        return new MobData(id, mob.type(), name, Math.max(1, Math.round(health)), mob.defence(),
                mob.attack() + floor.attack(), mob.attackCooldown(), damage, 0, mob.equipment(),
                mob.attributes(), mob.glowing() || variant.elite(), mob.drops());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object value) {
        return value instanceof List<?> rows ? (List<Map<String, Object>>) rows : List.of();
    }

    private static Number num(Object value) {
        if (value instanceof Number number) return number;
        if (value == null) return 0;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
