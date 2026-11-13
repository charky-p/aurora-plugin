package org.charky.aurora.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTest {
    @Test
    void cornersAreSortedWhateverOrderTheyComeIn() {
        Box box = new Box("world", 10, 70, -3, 2, 65, -20);

        assertEquals(new Box("world", 2, 65, -20, 10, 70, -3), box);
        assertEquals(2, box.x1());
        assertEquals(10, box.x2());
        assertEquals(-20, box.z1());
    }

    @Test
    void bothCornersAreInside() {
        Box box = new Box("world", 0, 70, 0, 2, 72, 2);

        assertTrue(box.contains("world", 0, 70, 0));
        assertTrue(box.contains("world", 2, 72, 2));
        assertFalse(box.contains("world", 3, 70, 0));
        assertFalse(box.contains("gormak", 1, 71, 1));
    }

    @Test
    void touchingBoxesOverlapNeighboursDoNot() {
        Box box = new Box("world", 0, 0, 0, 10, 10, 10);

        assertTrue(box.overlaps(new Box("world", 10, 0, 0, 15, 10, 10)));
        assertFalse(box.overlaps(new Box("world", 11, 0, 0, 15, 10, 10)));
        assertFalse(box.overlaps(new Box("gormak", 0, 0, 0, 10, 10, 10)));
    }

    @Test
    void volumeCountsBothCorners() {
        assertEquals(1, new Box("world", 5, 5, 5, 5, 5, 5).volume());
        assertEquals(9 * 9 * 5, new Box("world", 0, 0, 0, 8, 4, 8).volume());
    }
}
