package vn.ThatThingDev.task;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.ThatThingDev.Main;
import vn.ThatThingDev.hook.WorldGuardHook;
import vn.ThatThingDev.manager.ShardManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkTask extends BukkitRunnable {

    private final Main plugin;
    private final ShardManager shardManager;

    // Use ConcurrentHashMap to prevent Race Conditions
    private final Map<UUID, Integer> regionProgress = new ConcurrentHashMap<>();

    // Global tick counter (to avoid checking every tick)
    private int globalTickCounter = 0;

    public AfkTask(Main plugin) {
        this.plugin = plugin;
        this.shardManager = plugin.getShardManager();
    }

    public void removePlayer(UUID uuid) {
        regionProgress.remove(uuid);
    }

    @Override
    public void run() {
        // Load Config
        int rewardInterval = plugin.getConfig().getInt("afk-system.reward-interval", 60);

        // Region Config
        boolean regionEnabled = plugin.getConfig().getBoolean("afk-system.region-mode.enabled");
        List<String> regions = plugin.getConfig().getStringList("afk-system.region-mode.regions");
        double regionAmt = plugin.getConfig().getDouble("afk-system.region-mode.amount");

        // Global Config
        boolean globalEnabled = plugin.getConfig().getBoolean("afk-system.global-mode.enabled");
        double globalAmt = plugin.getConfig().getDouble("afk-system.global-mode.amount");

        // Temporary maps for Async processing
        Map<UUID, Double> pendingRewards = new HashMap<>();
        Map<UUID, String> pendingMessages = new HashMap<>();

        // Increment global counter (Task runs every 5s = 5 seconds added)
        globalTickCounter += 5;
        boolean triggerGlobal = globalTickCounter >= rewardInterval;
        if (triggerGlobal) globalTickCounter = 0; // Reset counter

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            boolean isInRegion = false;

            // --- LOGIC 1: REGION ACCUMULATION ---
            if (regionEnabled && WorldGuardHook.isInRegion(p, regions)) {
                isInRegion = true;

                // Add 5 seconds to progress
                int current = regionProgress.getOrDefault(uuid, 0) + 5;

                if (current >= rewardInterval) {
                    // Reward & Reset
                    regionProgress.put(uuid, 0);

                    double finalAmt = applyBooster(p, regionAmt);
                    pendingRewards.put(uuid, finalAmt);
                    pendingMessages.put(uuid, plugin.getConfig().getString("afk-system.region-mode.message"));
                } else {
                    // Not enough time yet -> Update progress
                    regionProgress.put(uuid, current);
                    p.sendActionBar(net.kyori.adventure.text.Component.text("§eAFK Zone: §f" + current + "/" + rewardInterval + "s"));
                }
            } else {
                // OPTIMIZATION FIX: Removed unnecessary 'containsKey' check.
                // Leaving the region resets progress immediately.
                regionProgress.remove(uuid);
            }

            // --- LOGIC 2: GLOBAL PERIODIC ---
            // Independent form Region logic (Mutually Exclusive)
            if (!isInRegion && globalEnabled && triggerGlobal) {
                double finalAmt = applyBooster(p, globalAmt);
                // Merge rewards if needed
                pendingRewards.merge(uuid, finalAmt, Double::sum);
                pendingMessages.putIfAbsent(uuid, plugin.getConfig().getString("afk-system.global-mode.message"));
            }
        }

        // --- ASYNC SAVE (Performance Optimization) ---
        if (!pendingRewards.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Map.Entry<UUID, Double> entry : pendingRewards.entrySet()) {
                        UUID uuid = entry.getKey();
                        double amount = entry.getValue();

                        // Save to Database
                        shardManager.addBalance(uuid, amount);

                        // Switch back to Sync to send messages safely
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null && p.isOnline()) {
                                String msg = pendingMessages.get(uuid);
                                if (msg != null) {
                                    String format = msg.replace("&", "§")
                                            .replace("%amount%", String.format("%.1f", amount));
                                    p.sendMessage(format);
                                }
                            }
                        });
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private double applyBooster(Player p, double base) {
        ConfigurationSection multipliers = plugin.getConfig().getConfigurationSection("afk.multipliers");
        if (multipliers != null) {
            for (String key : multipliers.getKeys(false)) {
                if (p.hasPermission("shards.booster." + key)) {
                    return base * multipliers.getDouble(key);
                }
            }
        }
        return base;
    }
}