package org.charky.aurora.core.database;

import org.charky.aurora.features.trader.TraderSpot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

// The traders table, one row for every spot a trader stands at
public class TraderStore {
    private final Database db;

    public TraderStore(Database db) {
        this.db = db;
    }

    // Returns the new spot's id, or -1 if the insert failed. Insert and id read share
    // a transaction so no other insert can land between them
    public int add(String traderId, String world, double x, double y, double z, float yaw) {
        int[] id = {-1};
        try {
            db.runTransaction(conn -> {
                Database.execute(conn, "INSERT INTO traders (trader, world, x, y, z, yaw) VALUES (?, ?, ?, ?, ?, ?)", pst -> {
                    pst.setString(1, traderId);
                    pst.setString(2, world);
                    pst.setDouble(3, x);
                    pst.setDouble(4, y);
                    pst.setDouble(5, z);
                    pst.setFloat(6, yaw);
                });
                try (PreparedStatement pst = conn.prepareStatement("SELECT last_insert_rowid()");
                     ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) id[0] = rs.getInt(1);
                }
            });
        } catch (SQLException e) {
            db.logger().log(Level.SEVERE, "Could not save trader '" + traderId + "'", e);
        }
        return id[0];
    }

    public void remove(int id) {
        db.update("DELETE FROM traders WHERE id = ?", pst -> pst.setInt(1, id));
    }

    public List<TraderSpot> loadAll() {
        List<TraderSpot> spots = new ArrayList<>();
        db.query("SELECT * FROM traders ORDER BY id", pst -> {}, rs -> spots.add(new TraderSpot(
                rs.getInt("id"),
                rs.getString("trader"),
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"))));
        return spots;
    }
}
