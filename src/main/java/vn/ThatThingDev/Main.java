package vn.ThatThingDev;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand; // Import thêm cái này
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import vn.ThatThingDev.command.ShardCommand;
import vn.ThatThingDev.database.DatabaseManager;
import vn.ThatThingDev.listener.PlayerKillListener;
import vn.ThatThingDev.manager.ShardManager;
import vn.ThatThingDev.papi.ShardPlaceholder;
import vn.ThatThingDev.task.AfkTask;

public class Main extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private ShardManager shardManager;

    @Override
    public void onEnable() {
        // 1. Config
        saveDefaultConfig();

        // 2. Database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 3. Manager
        shardManager = new ShardManager(databaseManager);

        // 4. Đăng ký Lệnh (Fix triệt để cảnh báo NullPointer)
        PluginCommand shardsCmd = getCommand("shards");
        if (shardsCmd != null) {
            shardsCmd.setExecutor(new ShardCommand(this));
        } else {
            getLogger().severe("Lỗi: Chưa đăng ký lệnh 'shards' trong plugin.yml!");
        }

        // 5. Đăng ký Sự kiện
        getServer().getPluginManager().registerEvents(new PlayerKillListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        // 6. Hook PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShardPlaceholder(this).register();
        }

        // 7. Start Task AFK
        long interval = getConfig().getLong("afk.interval-seconds", 300) * 20L;
        new AfkTask(this).runTaskTimer(this, interval, interval);

        getLogger().info("AfkShards v2.0 (Clean Build) loaded!");
    }

    @Override
    public void onDisable() {
        if (shardManager != null) {
            Bukkit.getOnlinePlayers().forEach(p ->
                    shardManager.unload(p.getUniqueId(), p.getName())
            );
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    // IntelliJ sẽ không báo vàng dòng này nữa nhờ SuppressWarnings
    @SuppressWarnings("unused")
    @EventHandler
    public void onJoinAsync(AsyncPlayerPreLoginEvent e) {
        if (shardManager != null) {
            shardManager.load(e.getUniqueId());
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (shardManager != null) {
            shardManager.unload(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        }
    }

    public ShardManager getShardManager() { return shardManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
}