package org.charky.aurora.features.moderation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationTest {
    // Each mute a mod gives the same player is longer, never past a week
    @Test
    void modMutesStepUpToAWeek() {
        assertEquals(15 * Durations.MINUTE, Durations.modMute(0));
        assertEquals(Durations.HOUR, Durations.modMute(1));
        assertEquals(Durations.DAY, Durations.modMute(2));
        assertEquals(3 * Durations.DAY, Durations.modMute(3));
        assertEquals(Durations.WEEK, Durations.modMute(4));
        assertEquals(Durations.WEEK, Durations.modMute(40));
    }

    @Test
    void lengthsParse() {
        assertEquals(30 * Durations.MINUTE, Durations.parse("30m"));
        assertEquals(2 * Durations.HOUR, Durations.parse("2h"));
        assertEquals(Durations.DAY, Durations.parse("1D"));
        assertEquals(2 * Durations.WEEK, Durations.parse("2w"));
        assertEquals(Durations.FOREVER, Durations.parse("perm"));
        assertEquals(-1, Durations.parse("spam"));
        assertEquals(-1, Durations.parse("0h"));
        assertEquals(-1, Durations.parse("h"));
    }

    @Test
    void lengthsRead() {
        assertEquals("15 minutes", Durations.describe(15 * Durations.MINUTE));
        assertEquals("1 week", Durations.describe(Durations.WEEK));
        assertEquals("forever", Durations.describe(Durations.FOREVER));
    }

    @Test
    void staffRanksGiveTheirPermissions() {
        assertEquals(Set.of(StaffPermissions.MOD), StaffPermissions.permissionsFor(Set.of("JUNIOR_MOD")));
        Set<String> builder = StaffPermissions.permissionsFor(Set.of("HEAD_BUILDER"));
        assertTrue(builder.contains(StaffPermissions.BUILDER));
        assertTrue(builder.contains("axiom.*"));
        assertFalse(builder.contains(StaffPermissions.MOD));
        assertTrue(StaffPermissions.permissionsFor(Set.of("ADMIN")).contains(StaffPermissions.ADMIN));
        assertTrue(StaffPermissions.permissionsFor(Set.of("HELPER", "TRAINEE")).isEmpty());
    }

    @Test
    void aPunishmentEndsWhenLiftedOrRunOut() {
        Punishment mute = new Punishment(1, java.util.UUID.randomUUID(), "x", Punishment.Type.MUTE, "", "mod", 0, 1000, false);
        assertTrue(mute.activeAt(500));
        assertFalse(mute.activeAt(1000));
        Punishment lifted = new Punishment(1, mute.uuid(), "x", Punishment.Type.BAN, "", "admin", 0, 0, true);
        assertFalse(lifted.activeAt(5));
        Punishment kick = new Punishment(1, mute.uuid(), "x", Punishment.Type.KICK, "", "mod", 0, 0, false);
        assertFalse(kick.activeAt(5));
    }
}
