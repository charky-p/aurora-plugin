package org.charky.aurora.core.database;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Two tables: player_cosmetics is what each player has bought or been given,
// player_equipped is what they wear in each slot. Cosmetics that come with
// playtime are not stored, they follow from the playtime on the profile.
// The maps are concurrent, chat reads them off the main thread
public class CosmeticStore {
    private final Database db;

    public CosmeticStore(Database db) {
        this.db = db;
    }

    public Map<UUID, Set<String>> loadOwned() {
        Map<UUID, Set<String>> results = new ConcurrentHashMap<>();
        db.query("SELECT uuid, cosmetic_id FROM player_cosmetics", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> ConcurrentHashMap.newKeySet())
                .add(rs.getString("cosmetic_id")));
        return results;
    }

    public void addOwned(UUID uuid, String cosmeticId) {
        db.update("INSERT OR IGNORE INTO player_cosmetics (uuid, cosmetic_id) VALUES (?, ?)", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, cosmeticId);
        });
    }

    public void removeOwned(UUID uuid, String cosmeticId) {
        db.update("DELETE FROM player_cosmetics WHERE uuid = ? AND cosmetic_id = ?", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, cosmeticId);
        });
    }

    public Map<UUID, Map<String, String>> loadEquipped() {
        Map<UUID, Map<String, String>> results = new ConcurrentHashMap<>();
        db.query("SELECT uuid, slot, cosmetic_id FROM player_equipped", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new ConcurrentHashMap<>())
                .put(rs.getString("slot"), rs.getString("cosmetic_id")));
        return results;
    }

    public void setEquipped(UUID uuid, String slot, String cosmeticId) {
        db.update("INSERT INTO player_equipped (uuid, slot, cosmetic_id) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid, slot) DO UPDATE SET cosmetic_id = excluded.cosmetic_id", pst -> {
            pst.setString(1, uuid.toString());
            pst.setString(2, slot);
            pst.setString(3, cosmeticId);
        });
    }

    // Everything the player has here, for a full reset
    public void deleteAll(UUID uuid) {
        db.update("DELETE FROM player_cosmetics WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
        db.update("DELETE FROM player_equipped WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()));
    }
}
