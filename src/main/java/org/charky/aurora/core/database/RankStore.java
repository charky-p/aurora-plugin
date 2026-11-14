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

    // A bought rank is kept for good, so each one is written once, straight away
    public void add(UUID uuid, String rankId) {
        db.update("INSERT OR IGNORE INTO player_ranks (uuid, rank_id) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, rankId);
        });
    }
}
