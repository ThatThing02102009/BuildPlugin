package vn.ThatThingDev.task;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.ThatThingDev.Main;
import vn.ThatThingDev.manager.ShardManager;

public class AfkTask extends BukkitRunnable {
    private final Main plugin;
    private final ShardManager shardManager;

    public AfkTask(Main plugin) {
        this.plugin = plugin;
        this.shardManager = plugin.getShardManager();
    }

    @Override
    public void run() {
        // Lấy giá trị cơ bản (Nếu lỗi config thì mặc định là 1.0)
        double base = plugin.getConfig().getDouble("afk.base-amount", 1.0);
        ConfigurationSection multipliers = plugin.getConfig().getConfigurationSection("afk.multipliers");

        for (Player p : Bukkit.getOnlinePlayers()) {
            double amount = base;

            // Check Rank Booster
            if (multipliers != null) {
                for (String key : multipliers.getKeys(false)) {
                    // Check quyền: shards.booster.vip, shards.booster.mvp...
                    if (p.hasPermission("shards.booster." + key)) {
                        double multiplier = multipliers.getDouble(key);
                        // Lấy mức thưởng cao nhất có thể
                        if (base * multiplier > amount) {
                            amount = base * multiplier;
                        }
                    }
                }
            }

            // Cộng tiền
            shardManager.addBalance(p.getUniqueId(), amount);

            // --- FIX LỖI NULL POINTER Ở ĐÂY ---
            // Thêm giá trị mặc định (tham số thứ 2) để không bao giờ bị null
            String msg = plugin.getConfig().getString("messages.afk-receive", "&a+ %amount% Shards");

            // Gửi Action Bar (An toàn 100%)
            p.sendActionBar(net.kyori.adventure.text.Component.text(
                    msg.replace("&", "§").replace("%amount%", String.valueOf(amount))
            ));
        }
    }
}