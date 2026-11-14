package org.charky.aurora.features.rank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Every rank there is, in the order the menus show them: the one everyone starts
// with, the ten earned by playtime, then the six bought with Essence
public final class Ranks {
    private static final long HOUR = 3600;

    public static final Rank MEMBER = new Rank("MEMBER", "<gray><b>MEMBER</b>", Rank.Kind.DEFAULT, 0);

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

    private static String display(String id, String gradient) {
        return "<gradient:" + gradient + "><b>" + id + "</b></gradient>";
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
