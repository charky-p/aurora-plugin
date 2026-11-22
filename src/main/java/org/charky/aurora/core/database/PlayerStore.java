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
            INSERT INTO playerData (uuid, rank, level, kills, mined, essence, xp, playtime)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                rank = excluded.rank,
                level = excluded.level,
                kills = excluded.kills,
                mined = excluded.mined,
                essence = excluded.essence,
                xp = excluded.xp,
                playtime = excluded.playtime;
            """;

    // Every main loadout (slot 1), under its player's own id: the one in play, or the one
    // put away under shadow while another is. Experimental and slots 2 and 3 never count
    private static final String MAIN_LOADOUTS = """
            (SELECT COALESCE(h.uuid, p.uuid) AS owner, p.* FROM playerData p
             LEFT JOIN loadout_homes h ON p.uuid = h.home
             LEFT JOIN loadouts l ON l.uuid = COALESCE(h.uuid, p.uuid)
             WHERE (h.home IS NULL AND (l.uuid IS NULL OR l.active = 'main'))
                OR (h.loadout = 'main' AND l.active <> 'main'))""";

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

    // The highest saved values of one column, best first, ties broken by a second one.
    // Each row is uuid, value, tiebreak. Columns come from LeaderboardStat, never a player
    public List<long[]> top(String column, String tiebreak, int limit) {
        List<long[]> rows = new ArrayList<>();
        db.query("SELECT owner, " + column + " AS v, " + tiebreak + " AS t FROM " + MAIN_LOADOUTS
                        + " ORDER BY v DESC, t DESC LIMIT ?",
                pst -> pst.setInt(1, limit),
                rs -> {
                    UUID uuid = UUID.fromString(rs.getString("owner"));
                    rows.add(new long[]{uuid.getMostSignificantBits(), uuid.getLeastSignificantBits(), rs.getLong("v"), rs.getLong("t")});
                });
        return rows;
    }

    // Where a value stands: how many saved players are ahead of it
    public int ahead(String column, long value) {
        int[] count = {0};
        db.query("SELECT COUNT(*) AS c FROM " + MAIN_LOADOUTS + " WHERE " + column + " > ?", pst -> pst.setLong(1, value),
                rs -> count[0] = rs.getInt("c"));
        return count[0];
    }

    // The highest saved levels, best first. Each row is uuid, level, xp
    public List<Object[]> topLevels(int limit) {
        List<Object[]> rows = new ArrayList<>();
        db.query("SELECT owner, level, xp FROM " + MAIN_LOADOUTS + " ORDER BY level DESC, xp DESC LIMIT ?",
                pst -> pst.setInt(1, limit),
                rs -> rows.add(new Object[]{UUID.fromString(rs.getString("owner")), rs.getInt("level"), rs.getLong("xp")}));
        return rows;
    }

    public PlayerData load(UUID uuid) {
        List<PlayerData> found = new ArrayList<>();
        db.query("SELECT * FROM playerData WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()),
                rs -> found.add(new PlayerData(
                        rs.getString("rank"),
                        rs.getInt("level"),
                        rs.getLong("xp"),
                        rs.getInt("kills"),
                        rs.getInt("mined"),
                        rs.getInt("essence"),
                        rs.getLong("playtime"))));

        return found.isEmpty() ? new PlayerData("MEMBER", 0, 0, 0, 0) : found.getFirst();
    }

    private static void bind(PreparedStatement pst, UUID uuid, PlayerData data) throws SQLException {
        pst.setString(1, uuid.toString());
        pst.setString(2, data.getRank());
        pst.setInt(3, data.getLevel());
        pst.setInt(4, data.getKills());
        pst.setInt(5, data.getMined());
        pst.setInt(6, data.getEssence());
        pst.setLong(7, data.getXp());
        pst.setLong(8, data.getPlaytime());
    }
}
