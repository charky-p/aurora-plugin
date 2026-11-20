package org.charky.aurora.common.util;

import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

// Trading what two keys hold, for a player's two loadouts. Safe for concurrent maps,
// which take no nulls: a key with nothing ends up with nothing
public final class Swaps {
    private Swaps() {
    }

    public static <K, V> void keys(Map<K, V> map, K a, K b) {
        V fromA = map.remove(a);
        V fromB = map.remove(b);
        if (fromB != null) map.put(a, fromB);
        if (fromA != null) map.put(b, fromA);
    }

    // What one key holds, copied to another by copier so the two never share it. A key
    // with nothing leaves the other with nothing
    public static <K, V> void copy(Map<K, V> map, K from, K to, UnaryOperator<V> copier) {
        V value = map.get(from);
        if (value == null) map.remove(to);
        else map.put(to, copier.apply(value));
    }

    public static <K> void members(Set<K> set, K a, K b) {
        boolean hadA = set.remove(a);
        boolean hadB = set.remove(b);
        if (hadB) set.add(a);
        if (hadA) set.add(b);
    }
}
