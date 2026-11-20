package org.charky.aurora.features.keys;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeysTest {
    private static Keys keys() {
        Keys keys = new Keys();
        keys.add("cobblestone", null, " ");
        keys.add("Gormak_Key", "GORMAK", null);
        keys.add("gormak_spare_key", "gormak", "");
        keys.add("eternal_sigil", null, "Gormak_Eternal");
        keys.add("khorvath_key", "khorvath", null);
        return keys;
    }

    @Test
    void onlyKeysAreKeys() {
        Keys keys = keys();
        assertEquals(List.of("gormak_key", "gormak_spare_key", "eternal_sigil", "khorvath_key"), keys.ids());
        assertTrue(keys.isKey("GORMAK_KEY"));
        assertTrue(keys.isKey("eternal_sigil"));
        assertFalse(keys.isKey("cobblestone"));
        assertFalse(keys.isKey(null));
    }

    @Test
    void passesGiveTags() {
        Keys keys = keys();
        assertEquals("gormak", keys.unlock("gormak_key"));
        assertNull(keys.unlock("eternal_sigil"));
        assertEquals(List.of("gormak", "khorvath"), keys.unlockTags());
        assertEquals(List.of("gormak_key", "gormak_spare_key"), keys.keysFor("Gormak"));
        assertEquals(List.of(), keys.keysFor("duat"));
    }

    @Test
    void summoningKeysCallMobs() {
        Keys keys = keys();
        assertEquals("gormak_eternal", keys.summon("eternal_sigil"));
        assertNull(keys.summon("gormak_key"));
        assertTrue(keys.isSummoned("GORMAK_ETERNAL"));
        assertFalse(keys.isSummoned("zombie"));
        assertFalse(keys.isSummoned(null));
    }

    // One item that is both a pass and a summon is listed once
    @Test
    void bothIsOneKey() {
        Keys keys = new Keys();
        keys.add("odd_key", "gormak", "boss");
        keys.add("odd_key", "gormak", "boss");
        assertEquals(List.of("odd_key"), keys.ids());
    }
}
