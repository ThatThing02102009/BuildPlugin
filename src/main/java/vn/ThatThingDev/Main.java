package vn.ThatThingDev;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
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
    private AfkTask afkTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 1. Database Init (Bây giờ đã trả về boolean nên dòng if này hoạt động OK)
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Khong the ket noi Database! Tat plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shardManager = new ShardManager(databaseManager);

        // 2. Register Commands
        PluginCommand cmd = getCommand("shards");
        if (cmd != null) cmd.setExecutor(new ShardCommand(this));

        // 3. Register Events
        getServer().getPluginManager().registerEvents(new PlayerKillListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        // 4. PlaceholderAPI Hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShardPlaceholder(this).register();
        }

        // 5. Start AFK Task (100 ticks = 5s)
        afkTask = new AfkTask(this);
        afkTask.runTaskTimer(this, 100L, 100L);

        getLogger().info("AfkShards v2.2-STABLE loaded successfully!");
    }

    @Override
    public void onDisable() {
        if (afkTask != null && !afkTask.isCancelled()) {
            afkTask.cancel();
        }

        if (shardManager != null) {
            Bukkit.getOnlinePlayers().forEach(p ->
                    shardManager.unload(p.getUniqueId(), p.getName())
            );
        }

        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    // Thêm dòng này để IntelliJ hết báo vàng "never used"
    @SuppressWarnings("unused")
    @EventHandler
    public void onJoinAsync(AsyncPlayerPreLoginEvent e) {
        if (shardManager != null) shardManager.load(e.getUniqueId());
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (shardManager != null) {
            shardManager.unload(e.getPlayer().getUniqueId(), e.getPlayer().getName());
        }
        if (afkTask != null) {
            afkTask.removePlayer(e.getPlayer().getUniqueId());
        }
    }

    public ShardManager getShardManager() { return shardManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
}