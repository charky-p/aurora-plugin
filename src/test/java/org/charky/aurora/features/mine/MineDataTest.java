package org.charky.aurora.features.mine;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class MineDataTest {
    private static MineData mine(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new MineData("test", "aurora_mines", x1, y1, z1, x2, y2, z2, 50, 600, 0, 0, 0);
    }

    // A stand-in world that only knows its name, enough for MineData
    private static World world(String name) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });
    }

    @Test
    void sharedBlockOverlaps() {
        MineData data = mine(0, 0, 0, 10, 10, 10);
        assertTrue(data.overlaps("aurora_mines", 5, 5, 5, 20, 20, 20));
        // Touching at x = 10, that column is in both
        assertTrue(data.overlaps("aurora_mines", 10, 0, 0, 15, 10, 10));
    }

    @Test
    void neighboursDoNotOverlap() {
        MineData data = mine(0, 0, 0, 10, 10, 10);
        assertFalse(data.overlaps("aurora_mines", 11, 0, 0, 15, 10, 10));
        assertFalse(data.overlaps("aurora_mines", 0, 11, 0, 10, 20, 10));
    }

    @Test
    void otherWorldsNeverOverlap() {
        assertFalse(mine(0, 0, 0, 10, 10, 10).overlaps("world", 0, 0, 0, 10, 10, 10));
    }

    @Test
    void containedBoxOverlaps() {
        assertTrue(mine(0, 0, 0, 10, 10, 10).overlaps("aurora_mines", 2, 2, 2, 3, 3, 3));
    }

    // Redefine used to store World.toString() and keep the corners unsorted
    @Test
    void setLocationsStoresTheWorldNameAndSortsCorners() {
        World world = world("aurora_mines");
        MineData data = mine(0, 0, 0, 1, 1, 1);
        data.setLocations(new Location(world, 10, 70, -3), new Location(world, 2, 65, -20));

        assertEquals("aurora_mines", data.getWorld());
        assertEquals(2, data.getX1());
        assertEquals(65, data.getY1());
        assertEquals(-20, data.getZ1());
        assertEquals(10, data.getX2());
        assertEquals(70, data.getY2());
        assertEquals(-3, data.getZ2());
    }
}
