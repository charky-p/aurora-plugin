package org.charky.aurora.features.gauntlet;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GauntletWeeklyTest {
    @Test
    void aWeekRunsMondayToSunday() {
        // Monday 16 to Sunday 22 November 2026 is the 47th week
        assertEquals(202647, GauntletWeekly.week(LocalDate.of(2026, 11, 16)));
        assertEquals(202647, GauntletWeekly.week(LocalDate.of(2026, 11, 22)));
        assertNotEquals(202647, GauntletWeekly.week(LocalDate.of(2026, 11, 23)));
    }

    @Test
    void theNewYearKeepsCounting() {
        // 1 January 2027 is a Friday, still in the last week of 2026
        assertEquals(202653, GauntletWeekly.week(LocalDate.of(2027, 1, 1)));
    }
}
