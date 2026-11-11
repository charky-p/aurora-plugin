package org.charky.aurora.core.database;

import org.charky.aurora.features.player.PlayerData;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

// The playerData table behind the scoreboard
public class PlayerStore {
    private static final String UPSERT_PLAYER = """
            INSERT INTO playerData (uuid, rank, level, kills, mined, essence)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                rank = excluded.rank,
                level = excluded.level,
                kills = excluded.kills,
                mined = excluded.mined,
                essence = excluded.essence;
            """;

    private final Database db;

    public PlayerStore(Database db) {
        this.db = db;
    }

    public void save(UUID uuid, PlayerData data) {
        if (data == null) return;
        db.update(UPSERT_PLAYER, pst -> bind(pst, uuid, data));
    }

    // Each profile is marked clean before its values are read, so a change made
    // mid-save stays dirty for the next one. A failed save marks them dirty again
    public void saveAll(ConcurrentMap<UUID, PlayerData> profiles) {
        List<PlayerData> saving = new ArrayList<>();
        try {
            db.runTransaction(conn -> {
                try (PreparedStatement pst = conn.prepareStatement(UPSERT_PLAYER)) {
                    for (Map.Entry<UUID, PlayerData> entry : profiles.entrySet()) {
                        PlayerData data = entry.getValue();
                        if (!data.isDirty()) continue;

                        data.resetDirty();
                        saving.add(data);
                        bind(pst, entry.getKey(), data);
                        pst.addBatch();
                    }
                    if (!saving.isEmpty()) pst.executeBatch();
                }
            });
        } catch (SQLException e) {
            saving.forEach(PlayerData::markDirty);
            db.logger().log(Level.SEVERE, "Could not save player data", e);
        }
    }

    public PlayerData load(UUID uuid) {
        List<PlayerData> found = new ArrayList<>();
        db.query("SELECT * FROM playerData WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()),
                rs -> found.add(new PlayerData(
                        rs.getString("rank"),
                        rs.getInt("level"),
                        rs.getInt("kills"),
                        rs.getInt("mined"),
                        rs.getInt("essence"))));

        return found.isEmpty() ? new PlayerData("MEMBER", 0, 0, 0, 0) : found.getFirst();
    }

    private static void bind(PreparedStatement pst, UUID uuid, PlayerData data) throws SQLException {
        pst.setString(1, uuid.toString());
        pst.setString(2, data.getRank());
        pst.setInt(3, data.getLevel());
        pst.setInt(4, data.getKills());
        pst.setInt(5, data.getMined());
        pst.setInt(6, data.getEssence());
    }
}
