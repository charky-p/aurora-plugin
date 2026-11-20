package org.charky.aurora.core.database;

import org.charky.aurora.features.loadout.Loadout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The loadouts table, which loadout each player has in play and the id the other one is
// put away under, and loadout_inventories, each loadout's inventory and ender chest from
// when it was last put away. A switch swaps every per-player table's rows between the
// player's own id and the put-away one, all in one transaction
public class LoadoutStore {
    // Every table of per-player data that belongs to a loadout. Builders and their
    // build inventories are the player's own and stay with them in both
    static final List<String> TABLES = List.of("playerData", "player_ranks", "player_cosmetics",
            "player_equipped", "player_tools", "player_perks", "collections", "backpack_data",
            "unlocked_gear", "portal_access", "tutorial");
    // Tables with a uuid that are not part of a loadout: a suggestion is the person's
    static final List<String> SHARED = List.of("builders", "build_inventories", "loadouts", "loadout_inventories",
            "suggestions");

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

    // Every row of the player's own id goes to the put-away one and back, through a
    // spare id so no key is ever taken twice, and the loadout now in play is noted
    public void swap(UUID uuid, UUID shadow, Loadout nowActive) {
        String own = uuid.toString();
        String away = shadow.toString();
        String spare = "swapping-" + own;
        db.transaction(conn -> {
            for (String table : TABLES) {
                Database.execute(conn, "UPDATE " + table + " SET uuid = ? WHERE uuid = ?", pst -> {
                    pst.setString(1, spare);
                    pst.setString(2, own);
                });
                Database.execute(conn, "UPDATE " + table + " SET uuid = ? WHERE uuid = ?", pst -> {
                    pst.setString(1, own);
                    pst.setString(2, away);
                });
                Database.execute(conn, "UPDATE " + table + " SET uuid = ? WHERE uuid = ?", pst -> {
                    pst.setString(1, away);
                    pst.setString(2, spare);
                });
            }
            Database.execute(conn, "INSERT OR REPLACE INTO loadouts (uuid, shadow, active) VALUES (?, ?, ?)", pst -> {
                pst.setString(1, own);
                pst.setString(2, away);
                pst.setString(3, nowActive.id());
            });
        });
    }
}
