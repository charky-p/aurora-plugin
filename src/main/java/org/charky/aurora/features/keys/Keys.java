package org.charky.aurora.features.keys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Which resources are keys, something that does a thing when right clicked: a pass
// gives a tag that locked portals ask for, a summoning key calls a mob. Ids and tags
// are lower case, in resources.csv order
public class Keys {
    // item id -> the tag right clicking it gives
    private final Map<String, String> unlocks = new LinkedHashMap<>();
    // item id -> the mob right clicking it summons
    private final Map<String, String> summons = new LinkedHashMap<>();
    private final List<String> ids = new ArrayList<>();

    // A resource as it loads, with its unlocks and summons columns. Not a key if both are blank
    public void add(String itemId, String unlocks, String summons) {
        String id = itemId.toLowerCase();
        boolean unlock = unlocks != null && !unlocks.isBlank();
        boolean summon = summons != null && !summons.isBlank();
        if (unlock) this.unlocks.put(id, unlocks.toLowerCase());
        if (summon) this.summons.put(id, summons.toLowerCase());
        if ((unlock || summon) && !ids.contains(id)) ids.add(id);
    }

    // Every key in data order
    public List<String> ids() {
        return List.copyOf(ids);
    }

    public boolean isKey(String itemId) {
        return unlock(itemId) != null || summon(itemId) != null;
    }

    // Every tag a key gives, the ones locked portals ask for
    public List<String> unlockTags() {
        return unlocks.values().stream().distinct().toList();
    }

    // The tag a key item gives, null if it is not a pass
    public String unlock(String itemId) {
        return itemId == null ? null : unlocks.get(itemId.toLowerCase());
    }

    // The mob a key item summons, null if it summons nothing
    public String summon(String itemId) {
        return itemId == null ? null : summons.get(itemId.toLowerCase());
    }

    // Summoned mobs only come from their key, their spawners never spawn them
    public boolean isSummoned(String mobId) {
        return mobId != null && summons.containsValue(mobId.toLowerCase());
    }

    // The items that give a tag, for telling players what they are missing
    public List<String> keysFor(String tag) {
        return unlocks.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(tag))
                .map(Map.Entry::getKey)
                .toList();
    }
}
