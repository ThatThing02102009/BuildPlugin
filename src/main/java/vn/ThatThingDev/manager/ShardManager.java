package vn.ThatThingDev.manager;

import vn.ThatThingDev.database.DatabaseManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShardManager {
    private final DatabaseManager db;
    // Cache RAM để xử lý nhanh
    private final Map<UUID, Double> balanceCache = new ConcurrentHashMap<>();

    public ShardManager(DatabaseManager db) {
        this.db = db;
    }

    public void load(UUID uuid) {
        // Dùng hàm loadPlayer mà bên kia bảo "never used" đấy
        db.loadPlayer(uuid).thenAccept(bal -> balanceCache.put(uuid, bal));
    }

    public void unload(UUID uuid, String name) {
        if (balanceCache.containsKey(uuid)) {
            // Dùng hàm savePlayer mà bên kia bảo "never used"
            db.savePlayer(uuid, name, balanceCache.get(uuid));
            balanceCache.remove(uuid);
        }
    }

    public double getBalance(UUID uuid) {
        return balanceCache.getOrDefault(uuid, 0.0);
    }

    public void setBalance(UUID uuid, double amount) {
        balanceCache.put(uuid, Math.max(0, amount));
    }

    public void addBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current >= amount) {
            setBalance(uuid, current - amount);
            return true;
        }
        return false;
    }
}