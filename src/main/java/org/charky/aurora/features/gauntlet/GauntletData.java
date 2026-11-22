package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.features.spawner.MobData;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

// gauntlet.yml, from scripts/data/gauntlet/ through generate.py: the floors, the gear
// and what each level of it costs, the boons, the care package levels and the
// milestones. Floor and gear numbers come from scripts/balance/gauntlet_balance.py
public record GauntletData(List<Floor> floors, Map<String, Gear> gear, List<GearLevel> levels,
                           Map<String, Boon> boons, List<CareLevel> care, List<Milestone> milestones,
                           Map<String, SetupItem> setup, Map<String, Talent> talents, Map<String, Mutator> mutators,
                           Map<String, Double> settings) {
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

    public record Gear(String id, String name, String item, List<String> materials, String kind, int damage,
                       int unlockShards, int unlockFloor) {
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

    public record Boon(String id, String name, String icon, String description, String stat, double amount, int max,
                       String rarity) {
        public String colour() {
            return switch (rarity) {
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

    // What can change a floor: its mobs' health, hits, speed, how many there are, how
    // often they are elite, and the percent more Shards the floor pays
    public record Mutator(String id, String name, String description, double health, double damage, double speed,
                          double count, double elites, int shards) {
    }

    public record Loot(String item, int min, int max, double chance) {
    }

    public record CareLevel(int level, int shards, int cores, List<Loot> loot) {
    }

    public record Milestone(int floor, int essence, String cosmetic) {
    }

    // A starting kit item, bought a level at a time for good: amount a level, handed
    // out at the start of every run. Level n costs n times shards and cores
    public record SetupItem(String id, String name, String item, int amount, int max, int shards, int cores,
                            String description) {
        public int shardsFor(int level) {
            return shards * level;
        }

        public int coresFor(int level) {
            return cores * level;
        }
    }

    public static GauntletData empty() {
        return new GauntletData(List.of(), Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    // Empty when the file is missing or broken, the gauntlet then refuses to start
    @SuppressWarnings("unchecked")
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
            if (root == null) return empty();

            List<Floor> floors = new ArrayList<>();
            for (Map<String, Object> row : list(root.get("floors"))) {
                Map<String, Integer> mobs = new LinkedHashMap<>();
                ((Map<String, Object>) row.get("mobs")).forEach((mob, count) -> {
                    if (knownMobs.contains(mob)) mobs.put(mob, num(count).intValue());
                    else plugin.getLogger().severe("GAUNTLET ERROR: no mob '" + mob + "'");
                });
                floors.add(new Floor(mobs, num(row.get("health")).doubleValue(), num(row.get("attack")).intValue(),
                        Math.max(1, num(row.get("kill")).intValue()), num(row.get("shards")).intValue(),
                        num(row.get("cores")).intValue()));
            }

            Map<String, Gear> gear = new LinkedHashMap<>();
            ((Map<String, Map<String, Object>>) root.getOrDefault("gear", Map.of())).forEach((id, row) ->
                    gear.put(id, new Gear(id, str(row.get("name")), str(row.get("item")),
                            ((List<Object>) row.get("materials")).stream().map(String::valueOf).toList(), str(row.get("kind")),
                            num(row.get("damage")).intValue(), num(row.get("unlock_shards")).intValue(),
                            num(row.get("unlock_floor")).intValue())));

            List<GearLevel> levels = new ArrayList<>();
            for (Map<String, Object> row : list(root.get("levels"))) {
                levels.add(new GearLevel(num(row.get("level")).intValue(), num(row.get("shards")).intValue(),
                        num(row.get("cores")).intValue(), num(row.get("material")).intValue(), num(row.get("melee")).intValue(),
                        num(row.get("ranged")).intValue(), num(row.get("defence")).intValue(),
                        num(row.get("protection")).intValue(), num(row.get("deflection")).intValue()));
            }

            Map<String, Boon> boons = new LinkedHashMap<>();
            ((Map<String, Map<String, Object>>) root.getOrDefault("boons", Map.of())).forEach((id, row) ->
                    boons.put(id, new Boon(id, str(row.get("name")), str(row.get("icon")), str(row.get("description")),
                            str(row.get("stat")), num(row.get("amount")).doubleValue(), num(row.get("max")).intValue(),
                            row.get("rarity") == null ? "common" : str(row.get("rarity")))));

            List<CareLevel> care = new ArrayList<>();
            for (Map<String, Object> row : list(root.get("care"))) {
                List<Loot> loot = new ArrayList<>();
                for (Map<String, Object> entry : list(row.get("loot"))) {
                    loot.add(new Loot(str(entry.get("item")), num(entry.get("min")).intValue(),
                            num(entry.get("max")).intValue(), num(entry.get("chance")).doubleValue()));
                }
                care.add(new CareLevel(num(row.get("level")).intValue(), num(row.get("shards")).intValue(),
                        num(row.get("cores")).intValue(), loot));
            }

            List<Milestone> milestones = new ArrayList<>();
            for (Map<String, Object> row : list(root.get("milestones"))) {
                milestones.add(new Milestone(num(row.get("floor")).intValue(), num(row.get("essence")).intValue(),
                        str(row.get("cosmetic"))));
            }
            Map<String, SetupItem> setup = new LinkedHashMap<>();
            ((Map<String, Map<String, Object>>) root.getOrDefault("setup", Map.of())).forEach((id, row) ->
                    setup.put(id, new SetupItem(id, str(row.get("name")), str(row.get("item")), num(row.get("amount")).intValue(),
                            num(row.get("max")).intValue(), num(row.get("shards")).intValue(), num(row.get("cores")).intValue(),
                            str(row.get("description")))));
            Map<String, Talent> talents = new LinkedHashMap<>();
            ((Map<String, Map<String, Object>>) root.getOrDefault("talents", Map.of())).forEach((id, row) ->
                    talents.put(id, new Talent(id, str(row.get("name")), str(row.get("icon")), str(row.get("description")),
                            str(row.get("stat")), num(row.get("amount")).doubleValue(), num(row.get("max")).intValue(),
                            num(row.get("shards")).intValue(), num(row.get("growth")).doubleValue(), num(row.get("cores")).intValue())));
            Map<String, Mutator> mutators = new LinkedHashMap<>();
            ((Map<String, Map<String, Object>>) root.getOrDefault("mutators", Map.of())).forEach((id, row) ->
                    mutators.put(id, new Mutator(id, str(row.get("name")), str(row.get("description")),
                            num(row.get("health")).doubleValue(), num(row.get("damage")).doubleValue(), num(row.get("speed")).doubleValue(),
                            num(row.get("count")).doubleValue(), num(row.get("elites")).doubleValue(), num(row.get("shards")).intValue())));
            Map<String, Double> settings = new LinkedHashMap<>();
            ((Map<String, Object>) root.getOrDefault("settings", Map.of())).forEach((key, value) -> settings.put(key, num(value).doubleValue()));
            return new GauntletData(List.copyOf(floors), gear, List.copyOf(levels), boons, List.copyOf(care),
                    List.copyOf(milestones), setup, talents, mutators, settings);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load " + FILE, e);
            return empty();
        }
    }

    // A tuning number from settings.csv, or the fallback when it is missing
    public double setting(String key, double fallback) {
        return settings.getOrDefault(key, fallback);
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

    public static String floorMobId(String mobId, int floor) {
        return mobId + FLOOR_SEP + floor;
    }

    // The base mob of a floor mob id, g_rotter from g_rotter#12
    public static String baseMobId(String floorMobId) {
        int sep = floorMobId.lastIndexOf(FLOOR_SEP);
        return sep < 0 ? floorMobId : floorMobId.substring(0, sep);
    }

    // A mob on its floor: the base mob's health times the floor's, its attack plus
    // the floor's. Null for an id that is not one
    public MobData variant(String id, Function<String, MobData> base) {
        int sep = id.lastIndexOf(FLOOR_SEP);
        if (sep < 0) return null;
        MobData mob = base.apply(id.substring(0, sep));
        int number;
        try {
            number = Integer.parseInt(id.substring(sep + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        Floor floor = floor(number);
        if (mob == null || floor == null) return null;
        Component name = mob.displayName().append(TextUtils.parse(" <dark_gray>F" + number));
        return new MobData(id, mob.type(), name, Math.round(mob.health() * floor.health()), mob.defence(),
                mob.attack() + floor.attack(), mob.attackCooldown(), mob.damage(), 0, mob.equipment(),
                mob.attributes(), mob.glowing(), mob.drops());
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
