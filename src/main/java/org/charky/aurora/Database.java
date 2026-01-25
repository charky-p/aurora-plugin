package org.charky.aurora;

import org.bukkit.Material;
import org.charky.aurora.models.MineBlock;
import org.charky.aurora.models.MineData;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private final String url;
    private Connection connection;

    public Database(String path) {
        this.url = "jdbc:sqlite:" + path + File.separator + "mines.db";
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) return;
        connection = DriverManager.getConnection(url);
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void setup() {
        try {
            connect();

            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS mines (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x1 INTEGER, y1 INTEGER, z1 INTEGER, " +
                    "x2 INTEGER, y2 INTEGER, z2 INTEGER, " +
                    "reset_threshold DOUBLE, " +
                    "tick_delay INTEGER)");

            // Blocks that map to mines, foreign key allows for efficient db normalisation on mine deletion
            statement.execute("CREATE TABLE IF NOT EXISTS blocks (" +
                    "mine TEXT, " +
                    "block TEXT, " +
                    "percent DOUBLE, " +
                    "FOREIGN KEY(mine) REFERENCES mines(name) ON DELETE CASCADE)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMine(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2, double threshold, int delay) {
        String sql = "INSERT INTO mines (name, world, x1, y1, z1, x2, y2, z2, reset_threshold, tick_delay) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection(); PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, world);
            pst.setInt(3, x1);
            pst.setInt(4, y1);
            pst.setInt(5, z1);
            pst.setInt(6, x2);
            pst.setInt(7, y2);
            pst.setInt(8, z2);
            pst.setDouble(9, threshold);
            pst.setInt(10, delay);

            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

//    public boolean mineExists(String name) {
//        String sql = "SELECT 1 FROM mines WHERE name = ?";
//        try (Connection conn = getConnection();
//             PreparedStatement pst = conn.prepareStatement(sql)) {
//            pst.setString(1, name);
//            try (ResultSet rs = pst.executeQuery()) {
//                return rs.next();
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    public List<MineData> getMines() {
        List<MineData> mines = new ArrayList<>();
        String sql = "SELECT * FROM mines";

        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                mines.add(new MineData(
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                        rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"),
                        rs.getDouble("reset_threshold"),
                        rs.getInt("reset_delay")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mines;
    }

    public Map<String, List<MineBlock>> getMineBlocks() {
        Map<String, List<MineBlock>> blockMap = new HashMap<>();
        String sql = "SELECT mine, block, percent FROM blocks";

        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                String mineName = rs.getString("mine");
                Material material = Material.matchMaterial(rs.getString("block"));
                double percent = rs.getDouble("percent");

                MineBlock mb = new MineBlock(material, percent);

                blockMap.computeIfAbsent(mineName, k -> new ArrayList<>()).add(mb);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return blockMap;
    }

    public void setBlocks(String name, List<MineBlock> blocks) {
        String deleteSql = "DELETE FROM blocks WHERE mine = ?";
        String insertSql = "INSERT INTO blocks (mine, block, percent) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Delete existing blocks
                try (PreparedStatement delPst = conn.prepareStatement(deleteSql)) {
                    delPst.setString(1, name);
                    delPst.executeUpdate();
                }

                // Batch insert new blocks
                try (PreparedStatement insPst = conn.prepareStatement(insertSql)) {
                    for (MineBlock block : blocks) {
                        insPst.setString(1, name);
                        insPst.setString(2, block.material().name());
                        insPst.setDouble(3, block.percentage());
                        insPst.addBatch();
                    }
                    insPst.executeBatch();
                }

                conn.commit();

            } catch (SQLException e) {
                // Undo deletion
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteBlocks(String name) {
        String sql = "DELETE FROM blocks WHERE mine = ?";

        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        connect();
        return connection;
    }
}
