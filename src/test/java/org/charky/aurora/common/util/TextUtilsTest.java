package org.charky.aurora.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextUtilsTest {
    private static final List<String> ITEMS = List.of("ghoul_scythe", "gormak_blade", "scout_blade", "blade_of_dawn", "tomb_key");

    @Test
    void completionsOnlyMatchTheStart() {
        assertEquals(List.of("ghoul_scythe", "gormak_blade"), TextUtils.getCompletions("G", ITEMS));
        assertEquals(List.of(), TextUtils.getCompletions("scythe", ITEMS));
    }

    @Test
    void searchFindsAnItemByAnyPartOfItsId() {
        assertEquals(List.of("ghoul_scythe"), TextUtils.searchCompletions("scythe", ITEMS));
    }

    @Test
    void searchPutsIdsThatStartWithTheInputFirst() {
        assertEquals(List.of("blade_of_dawn", "gormak_blade", "scout_blade"), TextUtils.searchCompletions("blade", ITEMS));
    }

    @Test
    void anEmptySearchListsEverythingOnce() {
        assertEquals(5, TextUtils.searchCompletions("", ITEMS).size());
    }
}
