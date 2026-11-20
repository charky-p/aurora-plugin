package org.charky.aurora.features.suggestion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SuggestionTest {
    @Test
    void spacesAreTidied() {
        assertEquals("add more mounts", Suggestion.clean("  add   more\tmounts "));
        assertEquals("", Suggestion.clean(null));
        assertEquals("", Suggestion.clean("   "));
    }

    @Test
    void emptyAndTooLongAreRefused() {
        assertNotNull(Suggestion.problem(""));
        assertNull(Suggestion.problem("a"));
        assertNull(Suggestion.problem("x".repeat(Suggestion.MAX_LENGTH)));
        assertNotNull(Suggestion.problem("x".repeat(Suggestion.MAX_LENGTH + 1)));
    }
}
