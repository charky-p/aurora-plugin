package org.charky.aurora.core.database;

import org.charky.aurora.core.Aurora;

import java.io.File;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// Async Database:
// - One connection, opened at startup and closed at shutdown
// - Every helper is synchronized, so only one thread uses the connection at a time.
//   Each store method is a single helper call, so stores need no locking of their own
// - async() hands work to one background thread that runs it in order, so a
//   player's save on quit always finishes before their load on rejoin
public class Database {
    private final Logger logger;
    private final String url;
    // A queue with one thread that takes jobs off it in order, reused for every job
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        // Named so it is easy to spot in crash logs and thread dumps
        Thread thread = new Thread(runnable, "Aurora-Database");
        // Daemon so an idle thread never stops the server from shutting down,
        // close() still waits for queued saves before the connection goes
        thread.setDaemon(true);
        return thread;
    });
    // Shared by every store, only ever opened by connect()
    private Connection connection;

    // A unit of work inside a transaction, may throw so the transaction rolls back
    @FunctionalInterface
    interface SqlWork {
        void run(Connection connection) throws SQLException;
    }

    // Fills in the ? placeholders of a statement
    @FunctionalInterface
    interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    // Called once for every row a query returns
    @FunctionalInterface
    interface RowReader {
        void read(ResultSet rs) throws SQLException;
    }

    public Database(Aurora plugin, String path) {
        File dbFile = new File(path, "mines.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        this.logger = plugin.getLogger();
    }

    // Runs the task on the database thread, after anything already queued.
    // Errors are logged instead of silently killing the job
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

    Logger logger() {
        return logger;
    }

    // A single INSERT, UPDATE, DELETE or CREATE, errors are logged
    synchronized void update(String sql, SqlBinder binder) {
        try {
            execute(connection(), sql, binder);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database update failed: " + sql, e);
        }
    }

    // A SELECT, hands each row to the reader, errors are logged
    synchronized void query(String sql, SqlBinder binder, RowReader reader) {
        try (PreparedStatement pst = connection().prepareStatement(sql)) {
            binder.bind(pst);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) reader.read(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database query failed: " + sql, e);
        }
    }

    // Several statements that must all succeed together, errors are logged
    void transaction(SqlWork work) {
        try {
            runTransaction(work);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database transaction failed", e);
        }
    }

    // All or nothing, rolls back on failure and always hands autocommit back.
    // Throws so a caller can react to the failure, transaction() just logs it
    synchronized void runTransaction(SqlWork work) throws SQLException {
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

    // Runs one statement and closes it, but never the connection itself
    static void execute(Connection conn, String sql, SqlBinder binder) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            binder.bind(pst);
            pst.executeUpdate();
        }
    }

    // The shared connection, reopened only if something closed it
    private Connection connection() throws SQLException {
        connect();
        return connection;
    }
}
