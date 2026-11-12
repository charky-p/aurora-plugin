package org.charky.aurora.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagerTest {
    @Test
    void anEmptyListStillHasOnePage() {
        assertEquals(1, Pager.pageCount(0));
    }

    @Test
    void aFullPageDoesNotStartTheNext() {
        assertEquals(1, Pager.pageCount(10));
        assertEquals(2, Pager.pageCount(11));
    }

    @Test
    void pagesRoundUp() {
        assertEquals(3, Pager.pageCount(21));
    }
}
