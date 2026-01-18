package vn.ThatThingDev.database; // QUAN TRỌNG: Phải đúng dòng này

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import vn.ThatThingDev.Main;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String host = plugin.getConfig().getString("mysql.host");
        String port = plugin.getConfig().getString("mysql.port");
        String dbName = plugin.getConfig().getString("mysql.database");
        String user = plugin.getConfig().getString("mysql.user");
        String password = plugin.getConfig().getString("mysql.password");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true");
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.addDataSourceProperty("cachePrepStmts", "true");

        dataSource = new HikariDataSource(config);
        CompletableFuture.runAsync(this::createTable);
    }

    private void createTable() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(16), " +
                    "balance DOUBLE DEFAULT 0)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating table: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    public CompletableFuture<Double> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT balance FROM player_shards WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getDouble("balance");
            } catch (SQLException e) { e.printStackTrace(); }
            return 0.0;
        });
    }

    public void savePlayer(UUID uuid, String username, double amount) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO player_shards (uuid, username, balance) VALUES (?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE balance = ?, username = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.setDouble(3, amount);
                ps.setDouble(4, amount);
                ps.setString(5, username);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public CompletableFuture<UUID> getUuidByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_shards WHERE username = ?")) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return UUID.fromString(rs.getString("uuid"));
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
        });
    }
}