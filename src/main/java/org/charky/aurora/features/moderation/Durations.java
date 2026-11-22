package org.charky.aurora.features.moderation;

import java.util.List;
import java.util.Locale;

// How long a mute or ban lasts. Pure numbers, tested directly
public final class Durations {
    public static final long MINUTE = 60_000L;
    public static final long HOUR = 60 * MINUTE;
    public static final long DAY = 24 * HOUR;
    public static final long WEEK = 7 * DAY;
    // Forever, for an admin's perm
    public static final long FOREVER = 0L;

    // A mod's mute of the same player gets longer each time, never past a week
    static final List<Long> MOD_MUTE_LADDER = List.of(15 * MINUTE, HOUR, DAY, 3 * DAY, WEEK);

    private Durations() {
    }

    // The mute a mod gives someone muted this many times before
    public static long modMute(int earlierMutes) {
        return MOD_MUTE_LADDER.get(Math.clamp(earlierMutes, 0, MOD_MUTE_LADDER.size() - 1));
    }

    // 30m, 2h, 1d, 1w or perm. -1 for anything else
    public static long parse(String text) {
        String value = text.toLowerCase(Locale.ROOT).trim();
        if (value.equals("perm") || value.equals("permanent") || value.equals("forever")) return FOREVER;
        if (value.length() < 2) return -1;
        long unit = switch (value.charAt(value.length() - 1)) {
            case 'm' -> MINUTE;
            case 'h' -> HOUR;
            case 'd' -> DAY;
            case 'w' -> WEEK;
            default -> -1;
        };
        if (unit < 0) return -1;
        try {
            long amount = Long.parseLong(value.substring(0, value.length() - 1));
            return amount > 0 ? amount * unit : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // "3 days", "1 hour", "forever"
    public static String describe(long millis) {
        if (millis == FOREVER) return "forever";
        long[] units = {WEEK, DAY, HOUR, MINUTE};
        String[] names = {"week", "day", "hour", "minute"};
        for (int i = 0; i < units.length; i++) {
            if (millis >= units[i]) {
                long amount = Math.round((double) millis / units[i]);
                return amount + " " + names[i] + (amount == 1 ? "" : "s");
            }
        }
        return "under a minute";
    }
}
