package org.charky.aurora.features.gauntlet;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.charky.aurora.common.util.TextUtils;
import org.charky.aurora.core.Aurora;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

// Leaderboards standing in the lobby: the top floors, PvP kills, duel ratings, the
// fastest solo runs to sprint_floor, the day's best runs, or the week's two boards. Each is a text display saved with the world (so deploys carry it),
// placed with /a gauntlet board and brought up to date every half minute
final class GauntletBoards {
    static final List<String> KINDS = List.of("floors", "pvp", "duels", "week", "sprint", "tower");
    private static final int ROWS = 10;
    // How far /a gauntlet board remove looks
    private static final double REACH = 8;
    private static final String TITLE = "<gradient:#FF416C:#FF4B2B><b>%s</b></gradient>";

    private final GauntletManager manager;
    private final NamespacedKey key;

    GauntletBoards(Aurora plugin, GauntletManager manager) {
        this.manager = manager;
        this.key = new NamespacedKey(plugin, "gauntlet_board");
    }

    // A board of this kind at the player's eyes
    void place(Player player, String kind) {
        player.getWorld().spawn(player.getEyeLocation(), TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(true);
            display.setLineWidth(250);
            display.setBackgroundColor(Color.fromARGB(110, 0, 0, 0));
            display.setPersistent(true);
            display.getPersistentDataContainer().set(key, PersistentDataType.STRING, kind);
            display.text(text(kind));
        });
    }

    // The nearest board within reach, gone. Its kind, or null without one
    String removeNearest(Player player) {
        Location at = player.getLocation();
        TextDisplay nearest = player.getNearbyEntities(REACH, REACH, REACH).stream()
                .filter(entity -> entity instanceof TextDisplay && entity.getPersistentDataContainer().has(key, PersistentDataType.STRING))
                .map(entity -> (TextDisplay) entity)
                .min(Comparator.comparingDouble((Entity entity) -> entity.getLocation().distanceSquared(at)))
                .orElse(null);
        if (nearest == null) return null;
        String kind = nearest.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        nearest.remove();
        return kind;
    }

    // Every board in the Gauntlet's world, while its chunks are loaded
    void refresh() {
        Location lobby = manager.arena().lobby();
        if (lobby == null || lobby.getWorld() == null) return;
        for (TextDisplay display : lobby.getWorld().getEntitiesByClass(TextDisplay.class)) {
            String kind = display.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (kind != null) display.text(text(kind));
        }
    }

    private Component text(String kind) {
        List<String> lines = new ArrayList<>();
        switch (kind) {
            case "floors" -> {
                lines.add(TITLE.formatted("TOP FLOORS"));
                lines.addAll(rows(manager.top(ROWS), profile -> "floor " + profile.best, "Nobody has cleared a floor yet"));
            }
            case "pvp" -> {
                lines.add(TITLE.formatted("PVP KILLS"));
                lines.addAll(rows(manager.topBy(GauntletPvp.KILLS, ROWS), profile -> profile.stat(GauntletPvp.KILLS) + " kills",
                        "Nobody has a PvP kill yet"));
            }
            case "tower" -> {
                lines.add(TITLE.formatted("BEST TODAY"));
                lines.addAll(rows(manager.daily().top(GauntletDaily.today(), ROWS), profile -> "floor " + profile.stat(GauntletDaily.BEST),
                        "No floor cleared today yet"));
            }
            case "sprint" -> {
                lines.add(TITLE.formatted("FASTEST TO FLOOR " + GauntletRuns.sprintFloor(manager.data())));
                lines.addAll(rows(manager.lowestBy(GauntletRuns.SPRINT, ROWS), profile -> GauntletRuns.clock(profile.stat(GauntletRuns.SPRINT)),
                        "Nobody has made it yet"));
            }
            case "duels" -> {
                lines.add(TITLE.formatted("DUEL RATING"));
                lines.addAll(rows(manager.topBy(GauntletDuels.RATING, ROWS), profile -> String.valueOf(profile.stat(GauntletDuels.RATING)),
                        "Nobody has fought a duel yet"));
            }
            default -> {
                GauntletWeekly weekly = manager.weekly();
                lines.add(TITLE.formatted("THIS WEEK"));
                lines.add("<gray>Paid out in <white>" + GauntletWeekly.timeLeft());
                lines.add("<yellow>Best floor");
                lines.addAll(rows(weekly.top(GauntletWeekly.FLOOR, 3), profile -> "floor " + profile.stat(GauntletWeekly.FLOOR),
                        "Nobody yet this week"));
                lines.add("<yellow>PvP wins");
                lines.addAll(rows(weekly.top(GauntletWeekly.PVP, 3), profile -> profile.stat(GauntletWeekly.PVP) + " wins",
                        "Nobody yet this week"));
            }
        }
        return TextUtils.parse(String.join("<newline>", lines));
    }

    // A board's rows, "1. name - value" with gold, silver and bronze for the top three,
    // or what to say while nobody is on it. The menu's boards too
    static List<String> rows(List<Map.Entry<UUID, GauntletProfile>> top, Function<GauntletProfile, String> value, String empty) {
        List<String> rows = new ArrayList<>();
        int place = 1;
        for (Map.Entry<UUID, GauntletProfile> entry : top) {
            String colour = switch (place) {
                case 1 -> "<gold>";
                case 2 -> "<#C0C0C0>";
                case 3 -> "<#CD7F32>";
                default -> "<gray>";
            };
            rows.add(colour + place++ + ". <white>" + GauntletManager.nameOf(entry.getKey()) + " <dark_gray>- " + colour
                    + value.apply(entry.getValue()));
        }
        if (rows.isEmpty()) rows.add("<gray>" + empty);
        return rows;
    }
}
