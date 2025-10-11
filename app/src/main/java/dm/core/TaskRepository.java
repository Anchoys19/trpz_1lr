package dm.core;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository implements AutoCloseable {
    private final Connection con;

    public TaskRepository(Path sqlitePath) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:" + sqlitePath.toAbsolutePath();
        this.con = DriverManager.getConnection(url);
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS tasks(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  url TEXT NOT NULL,
                  target TEXT NOT NULL,
                  status TEXT NOT NULL,
                  lastByte INTEGER NOT NULL DEFAULT 0,
                  totalBytes INTEGER NOT NULL DEFAULT -1
                );
                """);
        }
    }

    public int create(String url, String target) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tasks(url,target,status,lastByte,totalBytes) VALUES(?,?,'NEW',0,-1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, url);
            ps.setString(2, target);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No ID generated");
    }

    public void updateProgress(int id, long bytes, long total) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE tasks SET lastByte=?, totalBytes=?, status='RUNNING' WHERE id=?")) {
            ps.setLong(1, bytes);
            ps.setLong(2, total);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateStatus(int id, DownloadTask.Status st, long lastByte) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE tasks SET status=?, lastByte=? WHERE id=?")) {
            ps.setString(1, st.name());
            ps.setLong(2, lastByte);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public DownloadTask findById(int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM tasks WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<DownloadTask> listAll() throws SQLException {
        List<DownloadTask> out = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tasks ORDER BY id")) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    private static DownloadTask map(ResultSet rs) throws SQLException {
        return new DownloadTask(
                rs.getInt("id"),
                rs.getString("url"),
                Path.of(rs.getString("target")),
                DownloadTask.Status.valueOf(rs.getString("status")),
                rs.getLong("lastByte"),
                rs.getLong("totalBytes")
        );
    }

    public List<DownloadTask> listRange(int offset, int limit) throws SQLException {
        List<DownloadTask> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM tasks ORDER BY id LIMIT ? OFFSET ?")) {
            ps.setInt(1, Math.max(0, limit));
            ps.setInt(2, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }


    @Override public void close() throws Exception { con.close(); }
}
