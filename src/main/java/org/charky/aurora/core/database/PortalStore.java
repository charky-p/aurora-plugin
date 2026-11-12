package org.charky.aurora.core.database;

import org.charky.aurora.features.portal.Portal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// The portals table, one row per portal with its box and destination
public class PortalStore {
    private static final String REPLACE_PORTAL = "REPLACE INTO portals (name, world, x1, y1, z1, x2, y2, z2, "
            + "dest_world, dest_x, dest_y, dest_z, dest_yaw, dest_pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final Database db;

    public PortalStore(Database db) {
        this.db = db;
    }

    // Insert or overwrite, by name
    public void save(Portal portal) {
        db.update(REPLACE_PORTAL, pst -> bind(pst, portal));
    }

    public void remove(String name) {
        db.update("DELETE FROM portals WHERE name = ?", pst -> pst.setString(1, name));
    }

    public void rename(String oldName, String newName) {
        db.update("UPDATE portals SET name = ? WHERE name = ?", pst -> {
            pst.setString(1, newName);
            pst.setString(2, oldName);
        });
    }

    public List<Portal> loadAll() {
        List<Portal> portals = new ArrayList<>();
        db.query("SELECT * FROM portals", pst -> {}, rs -> {
            Portal portal = new Portal(
                    rs.getString("name"),
                    rs.getString("world"),
                    rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                    rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"));
            String destWorld = rs.getString("dest_world");
            if (destWorld != null) {
                portal.setDestination(destWorld,
                        rs.getDouble("dest_x"), rs.getDouble("dest_y"), rs.getDouble("dest_z"),
                        rs.getFloat("dest_yaw"), rs.getFloat("dest_pitch"));
            }
            portals.add(portal);
        });
        return portals;
    }

    private static void bind(PreparedStatement pst, Portal portal) throws SQLException {
        pst.setString(1, portal.getName());
        pst.setString(2, portal.getWorld());
        pst.setInt(3, portal.getX1());
        pst.setInt(4, portal.getY1());
        pst.setInt(5, portal.getZ1());
        pst.setInt(6, portal.getX2());
        pst.setInt(7, portal.getY2());
        pst.setInt(8, portal.getZ2());
        if (portal.hasDestination()) {
            pst.setString(9, portal.getDestWorld());
            pst.setDouble(10, portal.getDestX());
            pst.setDouble(11, portal.getDestY());
            pst.setDouble(12, portal.getDestZ());
            pst.setFloat(13, portal.getDestYaw());
            pst.setFloat(14, portal.getDestPitch());
        } else {
            for (int i = 9; i <= 14; i++) pst.setNull(i, Types.NULL);
        }
    }
}
