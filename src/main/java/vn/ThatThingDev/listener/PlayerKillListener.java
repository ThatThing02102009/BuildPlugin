package vn.ThatThingDev.listener;

// Đã xóa import Material thừa
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import vn.ThatThingDev.Main;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerKillListener implements Listener {

    private final Main plugin;
    private final Map<UUID, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    public PlayerKillListener(Main plugin) {
        this.plugin = plugin;
    }

    // Thêm dòng này để IntelliJ không báo vàng "never used" nữa
    @SuppressWarnings("unused")
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("kill-reward.enabled")) return;

        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        // Check cơ bản
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;

        // 1. CHỐNG FARM CLONE (Check trùng IP) - Đã Fix lỗi NullPointer
        if (plugin.getConfig().getBoolean("kill-reward.prevent-same-ip")) {
            // Kiểm tra xem địa chỉ IP có tồn tại không trước khi so sánh
            if (killer.getAddress() != null && victim.getAddress() != null) {
                if (killer.getAddress().getAddress().equals(victim.getAddress().getAddress())) {
                    killer.sendMessage(formatMsg("messages.kill-same-ip", "&cGian lận IP!"));
                    return;
                }
            }
        }

        // 2. CHỐNG SPAM KILL (Check Cooldown)
        long timeLeft = getCooldown(killer.getUniqueId(), victim.getUniqueId());
        if (timeLeft > 0) {
            killer.sendMessage(formatMsg("messages.kill-cooldown", "&cChờ!")
                    .replace("%time%", String.valueOf(timeLeft)));
            return;
        }

        // 3. TÍNH TOÁN THƯỞNG
        double amount = calculateReward(victim);

        // 4. TRAO THƯỞNG
        plugin.getShardManager().addBalance(killer.getUniqueId(), amount);
        killer.sendMessage(formatMsg("messages.kill-receive", "&a+ %amount%")
                .replace("%amount%", String.valueOf(amount))
                .replace("%victim%", victim.getName()));

        // 5. SET COOLDOWN MỚI
        setCooldown(killer.getUniqueId(), victim.getUniqueId());
    }

    private double calculateReward(Player victim) {
        double base = plugin.getConfig().getDouble("kill-reward.base-amount", 5.0);
        double bonus = 0;

        int netherite = 0;
        int diamond = 0;

        // Kiểm tra null cho item để tránh lỗi
        for (ItemStack item : victim.getInventory().getArmorContents()) {
            if (item == null || item.getType().isAir()) continue;

            String typeName = item.getType().name();
            if (typeName.startsWith("NETHERITE_")) netherite++;
            else if (typeName.startsWith("DIAMOND_")) diamond++;
        }

        if (netherite >= 3) bonus = plugin.getConfig().getDouble("kill-reward.armor-bonus.NETHERITE", 20.0);
        else if (diamond >= 3) bonus = plugin.getConfig().getDouble("kill-reward.armor-bonus.DIAMOND", 10.0);

        return base + bonus;
    }

    private long getCooldown(UUID killer, UUID victim) {
        if (!cooldowns.containsKey(killer)) return 0;
        Map<UUID, Long> map = cooldowns.get(killer);
        if (!map.containsKey(victim)) return 0;
        long left = (map.get(victim) - System.currentTimeMillis()) / 1000;
        return left > 0 ? left : 0;
    }

    private void setCooldown(UUID killer, UUID victim) {
        long time = System.currentTimeMillis() + (plugin.getConfig().getLong("kill-reward.cooldown-seconds") * 1000);
        cooldowns.computeIfAbsent(killer, k -> new ConcurrentHashMap<>()).put(victim, time);
    }

    private String formatMsg(String path, String def) {
        return plugin.getConfig().getString(path, def).replace("&", "§");
    }
}