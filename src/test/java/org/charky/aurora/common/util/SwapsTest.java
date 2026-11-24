package org.charky.aurora.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwapsTest {
    @Test
    void keysTrade() {
        Map<String, Integer> map = new ConcurrentHashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        Swaps.keys(map, "a", "b");
        assertEquals(Map.of("a", 2, "b", 1, "c", 3), map);
    }

    // Concurrent maps take no nulls, a key with nothing hands over nothing
    @Test
    void oneSideEmpty() {
        Map<String, Integer> map = new ConcurrentHashMap<>(Map.of("a", 1));
        Swaps.keys(map, "a", "b");
        assertEquals(Map.of("b", 1), map);
        Swaps.keys(map, "a", "b");
        assertEquals(Map.of("a", 1), map);
    }

    @Test
    void membersTrade() {
        Set<String> set = new HashSet<>(Set.of("a", "c"));
        Swaps.members(set, "a", "b");
        assertEquals(Set.of("b", "c"), set);
        Swaps.members(set, "c", "d");
        assertEquals(Set.of("b", "d"), set);
    }
}
