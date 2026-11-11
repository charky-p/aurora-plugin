package org.charky.aurora.common.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagedMenuTest {
    @Test
    void alwaysAtLeastOnePage() {
        assertEquals(1, PagedMenu.pageCount(0));
    }

    @Test
    void twentyEightEntriesFillAPage() {
        assertEquals(1, PagedMenu.pageCount(28));
        assertEquals(2, PagedMenu.pageCount(29));
        assertEquals(2, PagedMenu.pageCount(56));
        assertEquals(3, PagedMenu.pageCount(57));
    }
}
