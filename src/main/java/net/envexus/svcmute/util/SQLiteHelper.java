package net.envexus.svcmute.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteHelper {
    private static final String URL = "jdbc:sqlite:plugins/SVCMute/mutes.db";
    private static final Logger LOGGER = Logger.getLogger(SQLiteHelper.class.getName());

    private Connection connection;

    public SQLiteHelper() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS mutes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        unmute_time LONG NOT NULL,
                        reason TEXT,
                        executor TEXT
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS mute_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        mute_timestamp LONG NOT NULL,
                        duration TEXT NOT NULL,
                        reason TEXT,
                        executor TEXT
                    )
                    """);

            try {
                stmt.execute("ALTER TABLE mutes ADD COLUMN executor TEXT DEFAULT 'Unknown'");
            } catch (SQLException ignored) {
            }

            try {
                stmt.execute("ALTER TABLE mute_history ADD COLUMN executor TEXT DEFAULT 'Unknown'");
            } catch (SQLException ignored) {
            }

            try {
                stmt.execute("ALTER TABLE mutes ADD COLUMN reason TEXT DEFAULT 'No reason provided'");
            } catch (SQLException e) {
                if (!String.valueOf(e.getMessage()).contains("duplicate column name")) {
                    LOGGER.log(Level.SEVERE, "Failed to add reason column to mutes", e);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite database", e);
        }
    }

    private synchronized Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
            }
        }
        return connection;
    }

    public void addMute(String uuid, long unmuteTime, String reason, String executor) {
        String sql = "INSERT INTO mutes(uuid, unmute_time, reason, executor) VALUES(?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            pstmt.setLong(2, unmuteTime);
            pstmt.setString(3, reason);
            pstmt.setString(4, executor);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add mute for uuid=" + uuid, e);
        }
    }

    public void removeMute(String uuid) {
        String sql = "DELETE FROM mutes WHERE uuid = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to remove mute for uuid=" + uuid, e);
        }
    }

    public boolean isMuted(String uuid) {
        String sql = "SELECT unmute_time FROM mutes WHERE uuid = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long unmuteTime = rs.getLong("unmute_time");
                    return unmuteTime == -1 || unmuteTime > System.currentTimeMillis();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check mute state for uuid=" + uuid, e);
        }
        return false;
    }

    public List<MuteEntry> getAllActiveMutes() {
        List<MuteEntry> mutes = new ArrayList<>();
        String sql = "SELECT uuid, unmute_time, reason, executor FROM mutes WHERE unmute_time > ? OR unmute_time = -1";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, System.currentTimeMillis());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mutes.add(new MuteEntry(
                            rs.getString("uuid"),
                            rs.getLong("unmute_time"),
                            rs.getString("reason"),
                            rs.getString("executor")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load active mutes", e);
        }

        return mutes;
    }

    public void addMuteHistory(String uuid, long timestamp, String duration, String reason, String executor) {
        String sql = "INSERT INTO mute_history(uuid, mute_timestamp, duration, reason, executor) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            pstmt.setLong(2, timestamp);
            pstmt.setString(3, duration);
            pstmt.setString(4, reason);
            pstmt.setString(5, executor);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add mute history for uuid=" + uuid, e);
        }
    }

    public List<HistoryEntry> getMuteHistory(String uuid) {
        List<HistoryEntry> history = new ArrayList<>();
        String sql = """
                SELECT mute_timestamp, duration, reason, executor
                FROM mute_history
                WHERE uuid = ?
                ORDER BY mute_timestamp DESC
                """;

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new HistoryEntry(
                            rs.getLong("mute_timestamp"),
                            rs.getString("duration"),
                            rs.getString("reason") != null ? rs.getString("reason") : "No reason provided",
                            rs.getString("executor") != null ? rs.getString("executor") : "Unknown"
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load mute history for uuid=" + uuid, e);
        }

        return history;
    }

    public record MuteEntry(String uuid, long unmuteTime, String reason, String executor) {
    }

    public record HistoryEntry(long timestamp, String duration, String reason, String executor) {
    }
}