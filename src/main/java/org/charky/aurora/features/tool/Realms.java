package org.charky.aurora.features.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The realms of realms.csv. A realm is a group of worlds that shares one set of
// tool levels: the overworld, a mini world, an event world. A realm with a base
// builds on that realm's levels, one without stands alone. A world in no realm
// belongs to the first one. Pure, tested directly
public final class Realms {
    public record Realm(String id, String name, List<String> worlds, String base) {
    }

    private final Map<String, Realm> byId = new LinkedHashMap<>();

    public Realms(List<Realm> realms) {
        for (Realm realm : realms) byId.put(realm.id().toLowerCase(), realm);
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }

    public List<String> ids() {
        return new ArrayList<>(byId.keySet());
    }

    // Null for an id that is not a realm
    public Realm get(String id) {
        return id == null ? null : byId.get(id.toLowerCase());
    }

    // Null only when there are no realms at all
    public Realm ofWorld(String world) {
        for (Realm realm : byId.values()) {
            for (String name : realm.worlds()) {
                if (name.equalsIgnoreCase(world)) return realm;
            }
        }
        return byId.isEmpty() ? null : byId.values().iterator().next();
    }

    // The realm, then the one it builds on, and so on. A base that loops back or
    // does not exist ends the chain
    public List<Realm> chain(Realm realm) {
        List<Realm> chain = new ArrayList<>();
        for (Realm next = realm; next != null && !chain.contains(next); next = get(next.base())) {
            chain.add(next);
        }
        return chain;
    }
}
