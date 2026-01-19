package vn.ThatThingDev.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import vn.ThatThingDev.Main;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve", "unused", "UnusedReturnValue"})
public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" +
                    plugin.getConfig().getString("mysql.host") + ":" +
                    plugin.getConfig().getString("mysql.port") + "/" +
                    plugin.getConfig().getString("mysql.database"));
            config.setUsername(plugin.getConfig().getString("mysql.user"));
            config.setPassword(plugin.getConfig().getString("mysql.password"));

            config.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool-size", 10));
            config.setConnectionTimeout(plugin.getConfig().getLong("mysql.connection-timeout", 30000));

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            // Bước quan trọng: Tạo bảng và tự động cập nhật nếu thiếu cột
            setupTable();

            plugin.getLogger().info("Database connected & checked successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection failed:", e);
            return false;
        }
    }

    private void setupTable() {
        try (Connection conn = getConnection()) {
            // 1. Tao bang co ban (neu chua co)
            String sql = "CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "balance DOUBLE DEFAULT 0.0" +
                    ");";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.execute();
            }

            // 2. AUTO-MIGRATION (Tu dong va loi thieu cot 'name')
            // Day la buoc fix loi "Unknown column 'name'"
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "player_shards", "name");
            if (!rs.next()) {
                // Neu tim khong thay cot 'name' -> Them no vao ngay lap tuc
                plugin.getLogger().warning("Phat hien Database cu thieu cot 'name'. Dang tien hanh cap nhat...");
                String alterSql = "ALTER TABLE player_shards ADD COLUMN name VARCHAR(16) AFTER uuid";
                try (PreparedStatement ps = conn.prepareStatement(alterSql)) {
                    ps.execute();
                }
                plugin.getLogger().info("Da cap nhat Database thanh cong!");
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not setup SQL table:", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // --- ASYNC METHODS ---

    public CompletableFuture<Void> savePlayer(UUID uuid, String name, double balance) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_shards (uuid, name, balance) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name=?, balance=?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setDouble(3, balance);
                ps.setString(4, name);
                ps.setDouble(5, balance);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Error saving player " + name, e);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Async Save Error:", ex);
            return null;
        });
    }

    public CompletableFuture<Double> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM player_shards WHERE uuid=?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error loading player " + uuid, e);
            }
            return 0.0;
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Async Load Error:", ex);
            return 0.0;
        });
    }

    public CompletableFuture<UUID> getUuidByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid FROM player_shards WHERE name=?";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error finding UUID for " + name, e);
            }
            return null;
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Async UUID Lookup Error:", ex);
            return null;
        });
    }
}