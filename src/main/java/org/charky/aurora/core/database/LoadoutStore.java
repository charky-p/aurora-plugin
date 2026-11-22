package org.charky.aurora.core.database;

import org.charky.aurora.features.loadout.Loadout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The loadouts table, which loadout each player has in play and the id main is kept
// under while put away, and loadout_inventories, each loadout's inventory and ender chest from
// when it was last put away. A switch swaps every per-player table's rows between the
// player's own id and the put-away one, all in one transaction
public class LoadoutStore {
    // Every table of per-player data that belongs to a loadout. Builders and their
    // build inventories are the player's own and stay with them in both
    static final List<String> TABLES = List.of("playerData", "player_ranks", "player_cosmetics",
            "player_equipped", "player_tools", "player_perks", "collections", "backpack_data",
            "unlocked_gear", "portal_access", "tutorial", "quest", "fishing_rod");
    // Tables with a uuid that are not part of a loadout: a suggestion is the person's
    static final List<String> SHARED = List.of("builders", "build_inventories", "loadouts", "loadout_inventories", "loadout_homes", "punishments",
            "suggestions", "gauntlet_players", "gauntlet_gear", "gauntlet_milestones", "gauntlet_inventories", "gauntlet_setup", "gauntlet_layouts");

    // What a loadout was holding when it was put away
    public record Stash(byte[] items, byte[] enderChest) {
    }

    private final Database db;

    public LoadoutStore(Database db) {
        this.db = db;
    }

    // Only players who have ever switched, everyone else plays MAIN
    public Map<UUID, Loadout> loadActive() {
        Map<UUID, Loadout> active = new HashMap<>();
        db.query("SELECT uuid, active FROM loadouts", pst -> {},
                rs -> active.put(UUID.fromString(rs.getString("uuid")), Loadout.byId(rs.getString("active"))));
        return active;
    }

    public Map<UUID, Map<Loadout, Stash>> loadStashes() {
        Map<UUID, Map<Loadout, Stash>> stashes = new HashMap<>();
        db.query("SELECT uuid, loadout, items, ender FROM loadout_inventories", pst -> {},
                rs -> stashes.computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashMap<>())
                        .put(Loadout.byId(rs.getString("loadout")), new Stash(rs.getBytes("items"), rs.getBytes("ender"))));
        return stashes;
    }

    public void saveStash(UUID uuid, Loadout loadout, Stash stash) {
        db.update("INSERT OR REPLACE INTO loadout_inventories (uuid, loadout, items, ender) VALUES (?, ?, ?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, loadout.id());
            pst.setBytes(3, stash.items());
            pst.setBytes(4, stash.enderChest());
        });
    }

    // A new loadout that starts as a copy of the one in play: every row of the player's
    // own id copied to the put-away one, replacing whatever was there
    public void copy(UUID uuid, UUID shadow) {
        db.transaction(conn -> {
            for (String table : TABLES) {
                List<String> columns = new ArrayList<>();
                try (PreparedStatement pst = conn.prepareStatement("PRAGMA table_info(" + table + ")");
                     ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        if (!rs.getString("name").equalsIgnoreCase("uuid")) columns.add(rs.getString("name"));
                    }
                }
                String rest = String.join(", ", columns);
                Database.execute(conn, "DELETE FROM " + table + " WHERE uuid = ?", pst -> pst.setString(1, shadow.toString()));
                Database.execute(conn, "INSERT INTO " + table + " (uuid, " + rest + ") SELECT ?, " + rest
                        + " FROM " + table + " WHERE uuid = ?", pst -> {
                    pst.setString(1, shadow.toString());
                    pst.setString(2, uuid.toString());
                });
            }
        });
    }

    // From the loadout in play to another: its rows go to its own put-away id, then the
    // target's come from theirs to the player's own id, and the loadout now in play is
    // noted, all in one transaction. Each step is a swap, so a put-away id that was
    // somehow not empty is never lost
    public void switchTo(UUID uuid, UUID awayHome, UUID targetHome, Loadout nowActive) {
        String own = uuid.toString();
        db.transaction(conn -> {
            swapRows(conn, own, awayHome.toString());
            swapRows(conn, own, targetHome.toString());
            note(conn, uuid, nowActive);
        });
    }

    // Before slots 2 and 3, the one loadout put away was kept under Loadout.shadowOf, now
    // the experimental one's id. A player playing experimental then had main there: it
    // moves to its own id. The shadow column holds where main is kept once done, so this
    // runs once per player and again does nothing
    public int migrate() {
        Map<UUID, String> rows = new HashMap<>();
        db.query("SELECT uuid, shadow, active FROM loadouts", pst -> {}, rs -> {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            if (!Loadout.MAIN.homeOf(uuid).toString().equals(rs.getString("shadow"))) {
                rows.put(uuid, rs.getString("active"));
            }
        });
        if (rows.isEmpty()) return 0;
        db.transaction(conn -> {
            for (Map.Entry<UUID, String> row : rows.entrySet()) {
                UUID uuid = row.getKey();
                if (Loadout.EXPERIMENTAL.id().equalsIgnoreCase(row.getValue())) {
                    swapRows(conn, Loadout.shadowOf(uuid).toString(), Loadout.MAIN.homeOf(uuid).toString());
                }
                note(conn, uuid, Loadout.byId(row.getValue()));
            }
        });
        return rows.size();
    }

    // Every row of one id goes to the other and back, through a spare id so no key is
    // ever taken twice
    private static void swapRows(Connection conn, String a, String b) throws SQLException {
        String spare = "swapping-" + a;
        for (String table : TABLES) {
            Database.execute(conn, "UPDATE " + table + " SET uuid = ? WHERE uuid = ?", pst -> {
                pst.setString(1, spare);
                pst.setString(2, a);
            });
            Database.execute(conn, "UPDATE " + table + " SET uuid = ? WHERE uuid = ?", pst -> {
                pst.setString(1, a);
                pst.setString(2, b);
            });
            Database.execute(conn, "UPDATE " + table + " SET uuid = ? WHERE uuid = ?", pst -> {
                pst.setString(1, b);
                pst.setString(2, spare);
            });
        }
    }

    // The loadout in play, in shadow the id main is kept under while put away (which
    // marks the player as moved, see migrate), and every loadout's put-away id for the
    // leaderboards
    private static void note(Connection conn, UUID uuid, Loadout active) throws SQLException {
        Database.execute(conn, "INSERT OR REPLACE INTO loadouts (uuid, shadow, active) VALUES (?, ?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, Loadout.MAIN.homeOf(uuid).toString());
            pst.setString(3, active.id());
        });
        for (Loadout loadout : Loadout.values()) {
            Database.execute(conn, "INSERT OR REPLACE INTO loadout_homes (home, uuid, loadout) VALUES (?, ?, ?)", pst -> {
                pst.setString(1, loadout.homeOf(uuid).toString());
                pst.setString(2, uuid.toString());
                pst.setString(3, loadout.id());
            });
        }
    }
}
