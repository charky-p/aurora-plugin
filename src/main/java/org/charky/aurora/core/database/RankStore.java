package org.charky.aurora.core.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// The player_ranks table, one row per player per rank they bought. Playtime ranks
// are not stored, they follow from the playtime on the profile
public class RankStore {
    private final Database db;

    public RankStore(Database db) {
        this.db = db;
    }

    public Map<UUID, Set<String>> loadAll() {
        Map<UUID, Set<String>> results = new HashMap<>();
        db.query("SELECT uuid, rank_id FROM player_ranks", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new HashSet<>())
                .add(rs.getString("rank_id")));
        return results;
    }

    // The rank each player wears in their second slot
    public Map<UUID, String> loadSecond() {
        Map<UUID, String> results = new HashMap<>();
        db.query("SELECT uuid, rank_id FROM player_second_rank", pst -> {}, rs ->
                results.put(UUID.fromString(rs.getString("uuid")), rs.getString("rank_id")));
        return results;
    }

    // Null takes it off
    public void setSecond(UUID uuid, String rankId) {
        if (rankId == null) {
            db.update("DELETE FROM player_second_rank WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
            return;
        }
        db.update("INSERT INTO player_second_rank (uuid, rank_id) VALUES (?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET rank_id = excluded.rank_id", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, rankId);
        });
    }

    public void remove(UUID uuid, String rankId) {
        db.update("DELETE FROM player_ranks WHERE uuid = ? AND rank_id = ?", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, rankId);
        });
    }

    // A bought rank is kept for good, so each one is written once, straight away
    public void add(UUID uuid, String rankId) {
        db.update("INSERT OR IGNORE INTO player_ranks (uuid, rank_id) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, rankId);
        });
    }

    // Everything the player has here, for a full reset
    public void deleteAll(UUID uuid) {
        db.update("DELETE FROM player_ranks WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
        db.update("DELETE FROM player_second_rank WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }
}
