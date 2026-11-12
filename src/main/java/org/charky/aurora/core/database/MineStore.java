package org.charky.aurora.core.database;

import org.bukkit.Material;
import org.charky.aurora.features.mine.MineBlock;
import org.charky.aurora.features.mine.MineData;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The mines table and the blocks table that fills them
public class MineStore {
    private static final String REPLACE_MINE = "REPLACE INTO mines (name, world, x1, y1, z1, x2, y2, z2, "
            + "reset_threshold, delay, tpX, tpY, tpZ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final Database db;

    public MineStore(Database db) {
        this.db = db;
    }

    public void remove(String name) {
        db.transaction(conn -> {
            Database.execute(conn, "DELETE FROM mines WHERE name = ?", pst -> pst.setString(1, name));
            Database.execute(conn, "DELETE FROM blocks WHERE mine = ?", pst -> pst.setString(1, name));
        });
    }

    // Insert or overwrite, by name
    public void save(MineData data) {
        db.update(REPLACE_MINE, pst -> bind(pst, data));
    }

    // Blocks are keyed by mine name, so they move with it or the mine loads empty
    public void rename(String oldName, String newName) {
        db.transaction(conn -> {
            Database.execute(conn, "UPDATE mines SET name = ? WHERE name = ?", pst -> {
                pst.setString(1, newName);
                pst.setString(2, oldName);
            });
            Database.execute(conn, "UPDATE blocks SET mine = ? WHERE mine = ?", pst -> {
                pst.setString(1, newName);
                pst.setString(2, oldName);
            });
        });
    }

    public List<MineData> loadAll() {
        List<MineData> mines = new ArrayList<>();
        db.query("SELECT * FROM mines", pst -> {}, rs -> mines.add(new MineData(
                rs.getString("name"),
                rs.getString("world"),
                rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"),
                rs.getDouble("reset_threshold"),
                rs.getInt("delay"),
                rs.getDouble("tpX"), rs.getDouble("tpY"), rs.getDouble("tpZ"))));
        return mines;
    }

    public Map<String, List<MineBlock>> loadBlocks() {
        Map<String, List<MineBlock>> blockMap = new HashMap<>();
        db.query("SELECT mine, material, percent, id FROM blocks", pst -> {}, rs -> {
            Material material = Material.matchMaterial(rs.getString("material"));
            if (material == null) return;

            MineBlock block = new MineBlock(material, rs.getDouble("percent"), rs.getString("id"));
            blockMap.computeIfAbsent(rs.getString("mine"), k -> new ArrayList<>()).add(block);
        });
        return blockMap;
    }

    public void setBlocks(String name, List<MineBlock> blocks) {
        db.transaction(conn -> {
            Database.execute(conn, "DELETE FROM blocks WHERE mine = ?", pst -> pst.setString(1, name));

            try (PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO blocks (mine, material, percent, id) VALUES (?, ?, ?, ?)")) {
                for (MineBlock block : blocks) {
                    pst.setString(1, name);
                    pst.setString(2, block.material().name());
                    pst.setDouble(3, block.percentage());
                    pst.setString(4, block.id());
                    pst.addBatch();
                }
                pst.executeBatch();
            }
        });
    }

    public void setTp(String name, double tpX, double tpY, double tpZ) {
        db.update("UPDATE mines SET tpX = ?, tpY = ?, tpZ = ? WHERE name = ?", pst -> {
            pst.setDouble(1, tpX);
            pst.setDouble(2, tpY);
            pst.setDouble(3, tpZ);
            pst.setString(4, name);
        });
    }

    private static void bind(PreparedStatement pst, MineData data) throws SQLException {
        pst.setString(1, data.getName());
        pst.setString(2, data.getWorld());
        pst.setInt(3, data.getX1());
        pst.setInt(4, data.getY1());
        pst.setInt(5, data.getZ1());
        pst.setInt(6, data.getX2());
        pst.setInt(7, data.getY2());
        pst.setInt(8, data.getZ2());
        pst.setDouble(9, data.getThreshold());
        pst.setInt(10, data.getDelay());
        pst.setDouble(11, data.getTpX());
        pst.setDouble(12, data.getTpY());
        pst.setDouble(13, data.getTpZ());
    }
}
