package org.charky.aurora.features.rank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Every rank there is, in the order the menus show them: the one everyone starts
// with, the ten earned by playtime, the six bought with Essence, then the donor
// and staff ranks an admin hands out
public final class Ranks {
    private static final long HOUR = 3600;

    public static final Rank MEMBER = new Rank("MEMBER", "<gray><b>MEMBER</b>", Rank.Kind.DEFAULT, 0);
    // Not a rank itself: owning this id lets a player wear a second rank. It comes
    // with a donor package
    public static final String SECOND_SLOT = "RANK_SLOT_2";

    private static final Map<String, Rank> ALL = new LinkedHashMap<>();

    static {
        add(MEMBER);

        playtime("PLAYER", "#CFD8DC:#90A4AE", 1);
        playtime("ROOKIE", "#A8E063:#56AB2F", 5);
        playtime("APPRENTICE", "#56CCF2:#2F80ED", 10);
        playtime("NOVICE", "#38EF7D:#11998E", 25);
        playtime("EXPERT", "#FFD200:#F7971E", 50);
        playtime("NOBLE", "#C471ED:#F64F59", 100);
        playtime("LORD", "#F5AF19:#F12711", 150);
        playtime("DUKE", "#B06AB3:#4568DC", 250);
        playtime("MASTER", "#00F2FE:#4FACFE:#7F00FF", 350);
        playtime("GRANDMASTER", "#FFD700:#FFFFFF:#FFD700", 500);

        shop("EMBER", "#FF8008:#E52D27", 100);
        shop("FROST", "#FFFFFF:#6DD5FA", 300);
        shop("VERDANT", "#A8FF78:#0B8043", 750);
        shop("GILDED", "#FFF200:#D4AF37", 2000);
        shop("ECLIPSE", "#B47CFF:#3A1C71", 5000);
        // The server's own colours, the best there is
        shop("AURORA", "#7F7FD5:#86A8E7:#91EAE4", 15000);

        granted("LUMINARY", "#FFFF55:#FFAA00", Rank.Kind.DONOR);
        granted("CELESTIAL", "#654EA3:#EAAFC8", Rank.Kind.DONOR);

        // Lowest first, the way the team is laid out
        granted("TRAINEE", "#BDBDBD:#757575", Rank.Kind.STAFF);
        granted("HELPER", "#9FA8DA:#5C6BC0", Rank.Kind.STAFF);
        granted("SENIOR_HELPER", "#7986CB:#3F51B5", Rank.Kind.STAFF);
        granted("BUILDER", "#B2EBF2:#4DD0E1", Rank.Kind.STAFF);
        granted("SENIOR_BUILDER", "#80DEEA:#26C6DA", Rank.Kind.STAFF);
        granted("HEAD_BUILDER", "#4DD0E1:#00ACC1", Rank.Kind.STAFF);
        granted("JUNIOR_MOD", "#C8E6C9:#81C784", Rank.Kind.STAFF);
        granted("MOD", "#A5D6A7:#66BB6A", Rank.Kind.STAFF);
        granted("SENIOR_MOD", "#81C784:#43A047", Rank.Kind.STAFF);
        granted("HEAD_MOD", "#66BB6A:#2E7D32", Rank.Kind.STAFF);
        granted("JUNIOR_DEV", "#F8BBD0:#F48FB1", Rank.Kind.STAFF);
        granted("DEV", "#F48FB1:#EC407A", Rank.Kind.STAFF);
        granted("SENIOR_DEV", "#F06292:#E91E63", Rank.Kind.STAFF);
        granted("HEAD_DEV", "#EC407A:#C2185B", Rank.Kind.STAFF);
        granted("JUNIOR_ADMIN", "#FFAB91:#FF7043", Rank.Kind.STAFF);
        granted("ADMIN", "#EF5350:#C62828", Rank.Kind.STAFF);
        granted("SENIOR_ADMIN", "#E53935:#B71C1C", Rank.Kind.STAFF);
        granted("HEAD_ADMIN", "#D32F2F:#880E4F", Rank.Kind.STAFF);
        granted("MANAGER", "#FF8A65:#D84315", Rank.Kind.STAFF);
        granted("CO_FOUNDER", "#FFD54F:#FF6F00", Rank.Kind.STAFF);
        granted("FOUNDER", "#FFE082:#FFB300:#FF6F00", Rank.Kind.STAFF);
        granted("CO_OWNER", "#FF5555:#B71C1C", Rank.Kind.STAFF);
        granted("OWNER", "#AA0000:#FF5555", Rank.Kind.STAFF);
    }

    private Ranks() {
    }

    private static void add(Rank rank) {
        ALL.put(rank.id(), rank);
    }

    private static void playtime(String id, String gradient, long hours) {
        add(new Rank(id, display(id, gradient), Rank.Kind.PLAYTIME, hours * HOUR));
    }

    private static void shop(String id, String gradient, long essence) {
        add(new Rank(id, display(id, gradient), Rank.Kind.SHOP, essence));
    }

    private static void granted(String id, String gradient, Rank.Kind kind) {
        add(new Rank(id, display(id, gradient), kind, 0));
    }

    // SENIOR_MOD reads as SENIOR MOD
    private static String display(String id, String gradient) {
        return "<gradient:" + gradient + "><b>" + id.replace('_', ' ') + "</b></gradient>";
    }

    public static List<Rank> all() {
        return new ArrayList<>(ALL.values());
    }

    public static List<Rank> ofKind(Rank.Kind kind) {
        return ALL.values().stream().filter(rank -> rank.kind() == kind).toList();
    }

    // The rank with this id, MEMBER for one that no longer exists
    public static Rank get(String id) {
        Rank rank = id == null ? null : ALL.get(id.toUpperCase());
        return rank == null ? MEMBER : rank;
    }

    // The highest playtime rank this many seconds have earned, null for none yet
    public static Rank earnedBy(long seconds) {
        Rank best = null;
        for (Rank rank : ofKind(Rank.Kind.PLAYTIME)) {
            if (seconds >= rank.requirement()) best = rank;
        }
        return best;
    }
}
