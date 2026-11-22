package org.charky.aurora.core.data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The rules the progression rests on, checked against the generated YAML the plugin
// ships with. A CSV change that breaks one of them fails here, before a player finds
// a block nobody can mine or an upgrade that is worse than what it costs
class ProgressionDataTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");

    // The gear files, and the stat each kind of gear is for
    private static final Map<String, List<String>> PRIMARY_STATS = Map.of(
            "gear/weapons.yml", List.of("sharpness"),
            "gear/armor.yml", List.of("protection"),
            "gear/other.yml", List.of("deflection"));

    private record Trade(String shop, String item, List<String> costs) {
    }

    // Gear id to its enchant levels, and the file it came from
    private static final Map<String, Map<String, Integer>> gear = new HashMap<>();
    private static final Map<String, String> gearFile = new HashMap<>();
    private static final Set<String> resources = new HashSet<>();
    private static final Map<String, Map<String, Object>> specials = new HashMap<>();
    private static final Map<String, Map<String, Object>> blocks = new HashMap<>();
    private static final Map<String, Map<String, Object>> mobs = new HashMap<>();
    private static final List<Trade> trades = new ArrayList<>();

    @BeforeAll
    static void load() throws IOException {
        for (String file : PRIMARY_STATS.keySet()) {
            items(file).forEach((id, entry) -> {
                Map<String, Integer> levels = new HashMap<>();
                Object enchants = entry.get("enchants");
                if (enchants instanceof Map<?, ?> map) {
                    map.forEach((enchant, level) -> levels.put(enchant.toString(), ((Number) level).intValue()));
                }
                gear.put(id, levels);
                gearFile.put(id, file);
            });
        }

        resources.addAll(items("resources.yml").keySet());
        specials.putAll(items("resources_special.yml"));
        resources.addAll(specials.keySet());
        blocks.putAll(items("blocks.yml"));
        for (String file : List.of("mobs/origin.yml", "mobs/gormak.yml", "mobs/verdant.yml", "mobs/khorvath.yml",
                "mobs/elderwood.yml", "mobs/marsh.yml", "mobs/peaks.yml", "mobs/duat.yml", "mobs/sky.yml",
                "mobs/quarry.yml")) {
            mobs.putAll(items(file));
        }

        items("guis/shops.yml").forEach((shop, menu) -> {
            if (!(menu.get("components") instanceof Map<?, ?> components)) return;
            for (Object value : components.values()) {
                Map<?, ?> component = (Map<?, ?>) value;
                if (!"TRADE".equals(component.get("type"))) continue;

                List<String> costs = new ArrayList<>();
                if (component.get("cost") instanceof List<?> list) {
                    for (Object cost : list) costs.add(((Map<?, ?>) cost).get("id").toString());
                }
                trades.add(new Trade(shop, component.get("id").toString(), costs));
            }
        });
    }

    @Test
    void theDataLoaded() {
        assertFalse(gear.isEmpty());
        assertFalse(resources.isEmpty());
        assertFalse(blocks.isEmpty());
        assertFalse(mobs.isEmpty());
        assertFalse(trades.isEmpty());
    }

    @Test
    void everyTradeSellsAndCostsItemsThatExist() {
        List<String> problems = new ArrayList<>();
        for (Trade trade : trades) {
            if (!isItem(trade.item())) problems.add(trade.shop() + " sells unknown " + trade.item());
            for (String cost : trade.costs()) {
                if (!isItem(cost)) problems.add(trade.shop() + ": " + trade.item() + " costs unknown " + cost);
            }
        }
        assertNone(problems);
    }

    // A block of density d needs Fortune d, so the upgrades must take some tool that
    // far. Each realm counts its own levels from 1 and they add up, so the furthest a
    // tool gets is its best in every realm together, and the best tool is the bound.
    // Blocks do not say which tool or realm mines them, so this is an upper bound
    @Test
    void everyBlockCanBeMinedByAToolTheUpgradesReach() throws IOException {
        Map<String, Map<String, Integer>> bestByToolAndRealm = new HashMap<>();
        for (Map<String, Object> zone : items("upgrades.yml").values()) {
            if (!(zone.get("steps") instanceof List<?> steps)) continue;
            for (Object step : steps) {
                if (step instanceof Map<?, ?> map && "fortune".equals(map.get("stat"))) {
                    bestByToolAndRealm.computeIfAbsent(String.valueOf(map.get("tool")), tool -> new HashMap<>())
                            .merge(String.valueOf(zone.get("realm")), Integer.parseInt(map.get("level").toString()), Math::max);
                }
            }
        }
        int bestFortune = bestByToolAndRealm.values().stream()
                .mapToInt(byRealm -> byRealm.values().stream().mapToInt(Integer::intValue).sum())
                .max().orElse(0);

        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> block : blocks.entrySet()) {
            int density = block.getValue().get("density") instanceof Number number ? number.intValue() : 0;
            if (density > bestFortune) {
                problems.add(block.getKey() + " has density " + density + ", the upgrades stop at Fortune " + bestFortune);
            }
        }
        assertNone(problems);
    }

    @Test
    void everyBlockAndMobDropsItemsThatExist() {
        List<String> problems = new ArrayList<>();
        blocks.forEach((id, block) -> drops(block).forEach(drop -> {
            if (!isItem(drop)) problems.add("block " + id + " drops unknown " + drop);
        }));
        mobs.forEach((id, mob) -> drops(mob).forEach(drop -> {
            if (!isItem(drop)) problems.add("mob " + id + " drops unknown " + drop);
        }));
        assertNone(problems);
    }

    // A resource nothing drops can never be paid, so whatever costs it can never be bought
    @Test
    void everyResourceACostAsksForCanBeObtained() {
        Set<String> obtainable = new HashSet<>();
        blocks.values().forEach(block -> obtainable.addAll(drops(block)));
        mobs.values().forEach(mob -> obtainable.addAll(drops(mob)));
        for (Trade trade : trades) obtainable.add(trade.item());

        List<String> problems = new ArrayList<>();
        for (Trade trade : trades) {
            for (String cost : trade.costs()) {
                if (resources.contains(cost) && !obtainable.contains(cost)) {
                    problems.add(trade.item() + " costs " + cost + ", which nothing drops or sells");
                }
            }
        }
        assertNone(problems);
    }

    @Test
    void everyKeyCanBeObtainedAndSummonsAMobThatExists() {
        Set<String> obtainable = new HashSet<>();
        mobs.values().forEach(mob -> obtainable.addAll(drops(mob)));
        for (Trade trade : trades) obtainable.add(trade.item());

        List<String> problems = new ArrayList<>();
        specials.forEach((id, key) -> {
            if (!obtainable.contains(id)) problems.add("key " + id + " is never sold or dropped");

            Object summons = key.get("summons");
            if (summons != null && !mobs.containsKey(summons.toString())) {
                problems.add("key " + id + " summons unknown mob " + summons);
            }
        });
        assertNone(problems);
    }

    // What a mob wears and holds is looked up by id when it spawns, or is a plain
    // vanilla item named by its material in capitals (hand: BOW)
    @Test
    void everyMobWearsGearThatExists() {
        List<String> problems = new ArrayList<>();
        mobs.forEach((id, mob) -> {
            if (!(mob.get("equip") instanceof Map<?, ?> equip)) return;
            equip.forEach((slot, item) -> {
                if (item != null && !isItem(item.toString()) && org.bukkit.Material.matchMaterial(item.toString()) == null) {
                    problems.add("mob " + id + " wears unknown " + item + " on " + slot);
                }
            });
        });
        assertNone(problems);
    }

    private static boolean isItem(String id) {
        return gear.containsKey(id) || resources.contains(id);
    }

    @SuppressWarnings("unchecked")
    private static List<String> drops(Map<String, Object> entry) {
        List<String> ids = new ArrayList<>();
        if (entry.get("independent") instanceof List<?> independent) {
            for (Object drop : independent) ids.add(((Map<String, Object>) drop).get("id").toString());
        }
        if (entry.get("choices") instanceof Map<?, ?> choices && choices.get("options") instanceof List<?> options) {
            for (Object option : options) ids.add(((Map<String, Object>) option).get("id").toString());
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> items(String file) throws IOException {
        try (InputStream in = Files.newInputStream(RESOURCES.resolve(file))) {
            Map<String, Object> root = new Yaml().load(in);
            Object items = root == null ? null : root.get("items");
            assertTrue(items instanceof Map, file + " has no items");
            return (Map<String, Map<String, Object>>) items;
        }
    }

    private static void assertNone(List<String> problems) {
        assertEquals(List.of(), problems);
    }
}
