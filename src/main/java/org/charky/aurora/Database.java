package org.charky.aurora;

import org.bukkit.Material;
import org.charky.aurora.models.*;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class Database {
    private final String url;
    private Connection connection;

    public Database(String path) {
        File dbFile = new File(path, "mines.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
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
                    "delay INTEGER)");

            // Blocks that map to mines
            statement.execute("CREATE TABLE IF NOT EXISTS blocks (" +
                    "mine TEXT, " +
                    "block TEXT, " +
                    "percent DOUBLE)");

            // Player data for scoreboard
            statement.execute("CREATE TABLE IF NOT EXISTS playerData (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "rank TEXT DEFAULT 'MEMBER', " +
                    "level INTEGER DEFAULT 0, " +
                    "kills INTEGER DEFAULT 0, " +
                    "mined INTEGER DEFAULT 0, " +
                    "essence INTEGER DEFAULT 0)");

            // Mob Spawners
            statement.execute("CREATE TABLE IF NOT EXISTS spawners (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x INTEGER, y INTEGER, z INTEGER, " +
                    "mob TEXT, " +
                    "radius DOUBLE, " +
                    "mob_limit INT)");

            // TODO: Mob Entities to be spawned, need armor, weapon, other attributes, etc
//            statement.execute("CREATE TABLE IF NOT EXISTS mobs (" +
//                    "mob TEXT PRIMARY KEY, " +
//                    "type TEXT, " +
//                    "display_name TEXT, " +
//                    "max_health DOUBLE, " +
//                    "movement_speed DOUBLE, " +
//                    "helmet TEXT, " +
//                    "chestplate TEXT, " +
//                    "leggings TEXT, " +
//                    "boots TEXT, " +
//                    "weapon TEXT, " +
//                    "offhand TEXT, )");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMine(MineData data) {
        String sql = "INSERT INTO mines (name, world, x1, y1, z1, x2, y2, z2, reset_threshold, delay) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, data.name());
            pst.setString(2, data.world());
            pst.setInt(3, data.x1());
            pst.setInt(4, data.y1());
            pst.setInt(5, data.z1());
            pst.setInt(6, data.x2());
            pst.setInt(7, data.y2());
            pst.setInt(8, data.z2());
            pst.setDouble(9, data.threshold());
            pst.setInt(10, data.delay());

            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMine(String name) {
        String sqlMines = "DELETE FROM mines WHERE name = ?";
        String sqlBlocks = "DELETE FROM blocks WHERE mine = ?";

        try (Connection connection = getConnection()) {
            try (PreparedStatement pst = connection.prepareStatement(sqlMines)) {
                pst.setString(1, name);
                pst.executeUpdate();
            }

            try (PreparedStatement pst = connection.prepareStatement(sqlBlocks)) {
                pst.setString(1, name);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
                        rs.getInt("delay")
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

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                // Delete existing blocks
                try (PreparedStatement delPst = connection.prepareStatement(deleteSql)) {
                    delPst.setString(1, name);
                    delPst.executeUpdate();
                }

                // Batch insert new blocks
                try (PreparedStatement insPst = connection.prepareStatement(insertSql)) {
                    for (MineBlock block : blocks) {
                        insPst.setString(1, name);
                        insPst.setString(2, block.material().name());
                        insPst.setDouble(3, block.percentage());
                        insPst.addBatch();
                    }
                    insPst.executeBatch();
                }

                connection.commit();

            } catch (SQLException e) {
                // Undo deletion
                connection.rollback();
                e.printStackTrace();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerData(UUID uuid, PlayerData data) {
        // Upsert (delete and insert)
        String sql = """
        INSERT INTO playerData (uuid, rank, level, kills, mined, essence)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(uuid) DO UPDATE SET
            rank = excluded.rank,
            level = excluded.level,
            kills = excluded.kills,
            mined = excluded.mined,
            essence = excluded.essence;
        """;

        try (Connection connection = this.getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, uuid.toString());
            pst.setString(2, data.getRank());
            pst.setInt(3, data.getLevel());
            pst.setInt(4, data.getKills());
            pst.setInt(5, data.getMined());
            pst.setInt(6, data.getEssence());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAllPlayerData(ConcurrentMap<UUID, PlayerData> profiles) {
        String sql = """
        INSERT INTO playerData (uuid, rank, level, kills, mined, essence)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(uuid) DO UPDATE SET
            rank = excluded.rank,
            level = excluded.level,
            kills = excluded.kills,
            mined = excluded.mined,
            essence = excluded.essence;
        """;

        try (Connection connection = this.getConnection()) {
            connection.setAutoCommit(false);

            // Batch for all profiles
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                boolean change = false;

                for (Map.Entry<UUID, PlayerData> entry : profiles.entrySet()) {
                    PlayerData data = entry.getValue();

                    // Save only if changed
                    if (!data.isDirty()) continue;

                    pst.setString(1, entry.getKey().toString());
                    pst.setString(2, data.getRank());
                    pst.setInt(3, data.getLevel());
                    pst.setInt(4, data.getKills());
                    pst.setInt(5, data.getMined());
                    pst.setInt(6, data.getEssence());

                    pst.addBatch();
                    data.resetDirty();
                    change = true;
                }

                if (change) {
                    pst.executeBatch();
                    connection.commit();
                }
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
        String sql = "SELECT * FROM playerData WHERE uuid = ?";
        try (Connection connection = this.getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, uuid.toString());

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(
                            rs.getString("rank"),
                            rs.getInt("level"),
                            rs.getInt("kills"),
                            rs.getInt("mined"),
                            rs.getInt("essence")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new PlayerData("MEMBER", 0, 0, 0, 0);
    }

    public void saveSpawner(MobSpawner spawner) {
        String sql = "INSERT INTO spawners (name, world, x, y, z, mob, radius, mob_limit) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = this.getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, spawner.getName());
            pst.setString(2, spawner.getWorld());
            pst.setInt(3, spawner.getX());
            pst.setInt(4, spawner.getY());
            pst.setInt(5, spawner.getZ());
            pst.setString(6, spawner.getMobName());
            pst.setDouble(7, spawner.getRadius());
            pst.setInt(8, spawner.getLimit());

            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<MobSpawner> getSpawners(Aurora plugin) {
        String sql = "SELECT * FROM spawners";

        List<MobSpawner> spawners = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement pst = connection.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                String mob = rs.getString("mob");
                MobData data = MobPresets.get(mob);
                if (data == null) continue;

                spawners.add(new MobSpawner(
                        plugin,
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        data,
                        rs.getDouble("radius"),
                        rs.getInt("mob_limit")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return spawners;
    }

    public Connection getConnection() throws SQLException {
        connect();
        return connection;
    }
}
