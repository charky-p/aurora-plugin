package org.charky.aurora.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.charky.aurora.features.mine.MineBlock;
import org.charky.aurora.features.mine.MineData;
import org.charky.aurora.features.player.BackpackData;
import org.charky.aurora.features.player.PlayerData;
import org.charky.aurora.features.spawner.MobData;
import org.charky.aurora.features.spawner.MobSpawner;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// One SQLite connection for the plugin's lifetime. Every method is synchronized so
// the main thread and the database thread never share it mid-statement, and work
// sent through async() runs in order on a single thread, so a quit's save always
// lands before the same player's next load
public class Database {
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

    private static final String REPLACE_MINE = "REPLACE INTO mines (name, world, x1, y1, z1, x2, y2, z2, "
            + "reset_threshold, delay, tpX, tpY, tpZ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final DataRegistry registry;
    private final Logger logger;
    private final String url;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Aurora-Database");
        thread.setDaemon(true);
        return thread;
    });
    private Connection connection;

    @FunctionalInterface
    private interface SqlWork {
        void run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    public Database(Aurora plugin, String path) {
        File dbFile = new File(path, "mines.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        this.registry = plugin.getRegistry();
        this.logger = plugin.getLogger();
    }

    // Runs off the main thread, in submission order
    public void async(Runnable task) {
        worker.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Database task failed", e);
            }
        });
    }

    public synchronized void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) return;
        connection = DriverManager.getConnection(url);
    }

    // Lets queued saves finish before the connection goes
    public void close() throws SQLException {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Database thread did not finish in time, some saves may be lost");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) connection.close();
        }
    }

    public synchronized void setup() {
        try (Statement statement = connection().createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS mines (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x1 INTEGER, y1 INTEGER, z1 INTEGER, " +
                    "x2 INTEGER, y2 INTEGER, z2 INTEGER, " +
                    "reset_threshold DOUBLE, " +
                    "delay INTEGER, " +
                    "tpX DOUBLE, tpY DOUBLE, tpZ DOUBLE)");

            // Blocks that map to mines
            statement.execute("CREATE TABLE IF NOT EXISTS blocks (" +
                    "mine TEXT, " +
                    "material TEXT, " +
                    "percent DOUBLE, " +
                    "id TEXT)");

            // Player data for scoreboard
            statement.execute("CREATE TABLE IF NOT EXISTS playerData (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "rank TEXT DEFAULT 'MEMBER', " +
                    "level INTEGER DEFAULT 0, " +
                    "kills INTEGER DEFAULT 0, " +
                    "mined INTEGER DEFAULT 0, " +
                    "essence INTEGER DEFAULT 0)");

            // Player backpacks
            statement.execute("CREATE TABLE IF NOT EXISTS backpack_data (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "item_id TEXT NOT NULL, " +
                    "amount BIGINT NOT NULL, " +
                    "PRIMARY KEY (uuid, item_id))");

            // Mob Spawners
            statement.execute("CREATE TABLE IF NOT EXISTS spawners (" +
                    "name TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x INTEGER, y INTEGER, z INTEGER, " +
                    "mob TEXT, " +
                    "radius DOUBLE, " +
                    "mob_limit INTEGER, " +
                    "delay INTEGER)");

            // TODO: Traders
            statement.execute("CREATE TABLE IF NOT EXISTS traders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "trader TEXT, " +
                    "world TEXT, " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT)");

            // Spawns
            statement.execute("CREATE TABLE IF NOT EXISTS spawns (" +
                    "world TEXT PRIMARY KEY, " +
                    "x DOUBLE, y DOUBLE, z DOUBLE, " +
                    "yaw FLOAT, pitch FLOAT)");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not create database tables", e);
        }
    }

    public synchronized void addMine(MineData data) {
        update(REPLACE_MINE, pst -> bindMine(pst, data));
    }

    public synchronized void removeMine(String name) {
        transaction(conn -> {
            execute(conn, "DELETE FROM mines WHERE name = ?", pst -> pst.setString(1, name));
            execute(conn, "DELETE FROM blocks WHERE mine = ?", pst -> pst.setString(1, name));
        });
    }

    public synchronized void saveMine(MineData data) {
        update(REPLACE_MINE, pst -> bindMine(pst, data));
    }

    // Blocks are keyed by mine name, so they move with it or the mine loads empty
    public synchronized void renameMine(String oldName, String newName) {
        transaction(conn -> {
            execute(conn, "UPDATE mines SET name = ? WHERE name = ?", pst -> {
                pst.setString(1, newName);
                pst.setString(2, oldName);
            });
            execute(conn, "UPDATE blocks SET mine = ? WHERE mine = ?", pst -> {
                pst.setString(1, newName);
                pst.setString(2, oldName);
            });
        });
    }

    public synchronized List<MineData> getMines() {
        List<MineData> mines = new ArrayList<>();
        query("SELECT * FROM mines", pst -> {}, rs -> mines.add(new MineData(
                rs.getString("name"),
                rs.getString("world"),
                rs.getInt("x1"), rs.getInt("y1"), rs.getInt("z1"),
                rs.getInt("x2"), rs.getInt("y2"), rs.getInt("z2"),
                rs.getDouble("reset_threshold"),
                rs.getInt("delay"),
                rs.getDouble("tpX"), rs.getDouble("tpY"), rs.getDouble("tpZ"))));
        return mines;
    }

    public synchronized Map<String, List<MineBlock>> getMineBlocks() {
        Map<String, List<MineBlock>> blockMap = new HashMap<>();
        query("SELECT mine, material, percent, id FROM blocks", pst -> {}, rs -> {
            Material material = Material.matchMaterial(rs.getString("material"));
            if (material == null) return;

            MineBlock block = new MineBlock(material, rs.getDouble("percent"), rs.getString("id"));
            blockMap.computeIfAbsent(rs.getString("mine"), k -> new ArrayList<>()).add(block);
        });
        return blockMap;
    }

    public synchronized void setBlocks(String name, List<MineBlock> blocks) {
        transaction(conn -> {
            execute(conn, "DELETE FROM blocks WHERE mine = ?", pst -> pst.setString(1, name));

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

    public synchronized void setTp(String name, double tpX, double tpY, double tpZ) {
        update("UPDATE mines SET tpX = ?, tpY = ?, tpZ = ? WHERE name = ?", pst -> {
            pst.setDouble(1, tpX);
            pst.setDouble(2, tpY);
            pst.setDouble(3, tpZ);
            pst.setString(4, name);
        });
    }

    public synchronized void savePlayerData(UUID uuid, PlayerData data) {
        if (data == null) return;
        update(UPSERT_PLAYER, pst -> bindPlayer(pst, uuid, data));
    }

    // Each profile is marked clean before its values are read, so a change made
    // mid-save stays dirty for the next one. A failed save marks them dirty again
    public synchronized void saveAllPlayerData(ConcurrentMap<UUID, PlayerData> profiles) {
        List<PlayerData> saving = new ArrayList<>();
        try {
            runTransaction(conn -> {
                try (PreparedStatement pst = conn.prepareStatement(UPSERT_PLAYER)) {
                    for (Map.Entry<UUID, PlayerData> entry : profiles.entrySet()) {
                        PlayerData data = entry.getValue();
                        if (!data.isDirty()) continue;

                        data.resetDirty();
                        saving.add(data);
                        bindPlayer(pst, entry.getKey(), data);
                        pst.addBatch();
                    }
                    if (!saving.isEmpty()) pst.executeBatch();
                }
            });
        } catch (SQLException e) {
            saving.forEach(PlayerData::markDirty);
            logger.log(Level.SEVERE, "Could not save player data", e);
        }
    }

    public synchronized PlayerData loadPlayerData(UUID uuid) {
        List<PlayerData> found = new ArrayList<>();
        query("SELECT * FROM playerData WHERE uuid = ?", pst -> pst.setString(1, uuid.toString()),
                rs -> found.add(new PlayerData(
                        rs.getString("rank"),
                        rs.getInt("level"),
                        rs.getInt("kills"),
                        rs.getInt("mined"),
                        rs.getInt("essence"))));

        return found.isEmpty() ? new PlayerData("MEMBER", 0, 0, 0, 0) : found.getFirst();
    }

    public synchronized void saveSpawner(MobSpawner spawner) {
        update("INSERT INTO spawners (name, world, x, y, z, mob, radius, mob_limit, delay) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", pst -> {
            pst.setString(1, spawner.getName());
            pst.setString(2, spawner.getWorld());
            pst.setInt(3, spawner.getX());
            pst.setInt(4, spawner.getY());
            pst.setInt(5, spawner.getZ());
            pst.setString(6, spawner.getMobName());
            pst.setDouble(7, spawner.getRadius());
            pst.setInt(8, spawner.getLimit());
            pst.setInt(9, spawner.getDelay());
        });
    }

    public synchronized void removeSpawner(String spawnerName) {
        update("DELETE FROM spawners WHERE name = ?", pst -> pst.setString(1, spawnerName));
    }

    public synchronized List<MobSpawner> getSpawners(Aurora plugin) {
        List<MobSpawner> spawners = new ArrayList<>();
        query("SELECT * FROM spawners", pst -> {}, rs -> {
            MobData data = registry.getMob(rs.getString("mob"));
            if (data == null) return;

            spawners.add(new MobSpawner(
                    plugin,
                    rs.getString("name"),
                    rs.getString("world"),
                    rs.getInt("x"),
                    rs.getInt("y"),
                    rs.getInt("z"),
                    data,
                    rs.getDouble("radius"),
                    rs.getInt("mob_limit"),
                    rs.getInt("delay")));
        });
        return spawners;
    }

    public synchronized void setMob(String spawnerName, String mobName) {
        updateSpawner("mob", spawnerName, pst -> pst.setString(1, mobName));
    }

    public synchronized void setSpawnerRadius(String spawnerName, double radius) {
        updateSpawner("radius", spawnerName, pst -> pst.setDouble(1, radius));
    }

    public synchronized void setSpawnerLimit(String spawnerName, int limit) {
        updateSpawner("mob_limit", spawnerName, pst -> pst.setInt(1, limit));
    }

    public synchronized void setSpawnerDelay(String spawnerName, int delay) {
        updateSpawner("delay", spawnerName, pst -> pst.setInt(1, delay));
    }

    public synchronized void spawnerRedefine(String spawnerName, int x, int y, int z) {
        update("UPDATE spawners SET x = ?, y = ?, z = ? WHERE name = ?", pst -> {
            pst.setInt(1, x);
            pst.setInt(2, y);
            pst.setInt(3, z);
            pst.setString(4, spawnerName);
        });
    }

    public synchronized void spawnerRename(String oldName, String newName) {
        updateSpawner("name", oldName, pst -> pst.setString(1, newName));
    }

    public synchronized Map<World, Location> getSpawns() {
        Map<World, Location> spawnMap = new HashMap<>();
        query("SELECT * FROM spawns", pst -> {}, rs -> {
            // A world that is not loaded has nowhere to put the spawn
            World world = Bukkit.getWorld(rs.getString("world"));
            if (world == null) return;

            spawnMap.put(world, new Location(
                    world,
                    rs.getDouble("x"),
                    rs.getDouble("y"),
                    rs.getDouble("z"),
                    rs.getFloat("yaw"),
                    rs.getFloat("pitch")));
        });
        return spawnMap;
    }

    public synchronized void saveSpawn(Location location) {
        update("INSERT INTO spawns (world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(world) DO UPDATE SET x=excluded.x, y=excluded.y, z=excluded.z, " +
                "yaw=excluded.yaw, pitch=excluded.pitch", pst -> {
            pst.setString(1, location.getWorld().getName());
            pst.setDouble(2, location.getX());
            pst.setDouble(3, location.getY());
            pst.setDouble(4, location.getZ());
            pst.setFloat(5, location.getYaw());
            pst.setFloat(6, location.getPitch());
        });
    }

    public synchronized Map<UUID, BackpackData> loadAllBackpacks(DataRegistry registry) {
        Map<UUID, BackpackData> results = new HashMap<>();
        query("SELECT uuid, item_id, amount FROM backpack_data", pst -> {}, rs -> results
                .computeIfAbsent(UUID.fromString(rs.getString("uuid")), k -> new BackpackData(registry))
                .addItem(rs.getString("item_id"), rs.getLong("amount")));
        return results;
    }

    // Takes a copy made on the main thread, so it is safe to save from the database thread
    public synchronized void saveBackpacks(Map<UUID, Map<String, Long>> backpacks) {
        transaction(conn -> {
            // Wipe to refresh state
            execute(conn, "DELETE FROM backpack_data", pst -> {});

            try (PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO backpack_data (uuid, item_id, amount) VALUES (?, ?, ?)")) {
                for (Map.Entry<UUID, Map<String, Long>> entry : backpacks.entrySet()) {
                    for (Map.Entry<String, Long> item : entry.getValue().entrySet()) {
                        if (item.getValue() <= 0) continue;

                        pst.setString(1, entry.getKey().toString());
                        pst.setString(2, item.getKey());
                        pst.setLong(3, item.getValue());
                        pst.addBatch();
                    }
                }
                pst.executeBatch();
            }
        });
    }

    private Connection connection() throws SQLException {
        connect();
        return connection;
    }

    private void bindMine(PreparedStatement pst, MineData data) throws SQLException {
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

    private void bindPlayer(PreparedStatement pst, UUID uuid, PlayerData data) throws SQLException {
        pst.setString(1, uuid.toString());
        pst.setString(2, data.getRank());
        pst.setInt(3, data.getLevel());
        pst.setInt(4, data.getKills());
        pst.setInt(5, data.getMined());
        pst.setInt(6, data.getEssence());
    }

    // Column names are only ever our own constants, never player input
    private void updateSpawner(String column, String spawnerName, SqlBinder value) {
        update("UPDATE spawners SET " + column + " = ? WHERE name = ?", pst -> {
            value.bind(pst);
            pst.setString(2, spawnerName);
        });
    }

    private void update(String sql, SqlBinder binder) {
        try {
            execute(connection(), sql, binder);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database update failed: " + sql, e);
        }
    }

    private static void execute(Connection conn, String sql, SqlBinder binder) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            binder.bind(pst);
            pst.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface RowReader {
        void read(ResultSet rs) throws SQLException;
    }

    private void query(String sql, SqlBinder binder, RowReader reader) {
        try (PreparedStatement pst = connection().prepareStatement(sql)) {
            binder.bind(pst);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) reader.read(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database query failed: " + sql, e);
        }
    }

    private void transaction(SqlWork work) {
        try {
            runTransaction(work);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database transaction failed", e);
        }
    }

    // All or nothing, rolls back on failure and always hands autocommit back
    private void runTransaction(SqlWork work) throws SQLException {
        Connection conn = connection();
        conn.setAutoCommit(false);
        try {
            work.run(conn);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
