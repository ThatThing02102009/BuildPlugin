package vn.ThatThingDev.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import vn.ThatThingDev.Main;
import vn.ThatThingDev.manager.ShardManager;

public class ShardCommand implements CommandExecutor {
    private final Main plugin;
    private final ShardManager shardManager;

    public ShardCommand(Main plugin) {
        this.plugin = plugin;
        this.shardManager = plugin.getShardManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. Help Menu
        if (args.length == 0) {
            sender.sendMessage("§e--------- AfkShards v2.0 ---------");
            sender.sendMessage("§6/shards bal §7- Xem số dư của bạn");
            if (sender.hasPermission("shards.admin")) {
                sender.sendMessage("§c/shards admin reload §7- Tải lại Config");
                sender.sendMessage("§c/shards admin set/give/withdraw <player> <amount>");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // 2. Lệnh xem tiền (/shards bal)
        if (sub.equals("bal") || sub.equals("balance")) {
            if (sender instanceof Player p) {
                double bal = shardManager.getBalance(p.getUniqueId());
                String msg = plugin.getConfig().getString("messages.balance", "&7Số dư Shards: &e%amount%");
                sender.sendMessage(msg.replace("&", "§").replace("%amount%", String.valueOf(bal)));
            }
            return true;
        }

        // 3. Admin Commands
        if (sender.hasPermission("shards.admin")) {

            // RELOAD
            if (sub.equals("reload") || (args.length > 1 && args[1].equalsIgnoreCase("reload"))) {
                plugin.reloadConfig();
                String reloadMsg = plugin.getConfig().getString("messages.reload", "&a[AfkShards] Config reloaded!");
                sender.sendMessage(reloadMsg.replace("&", "§"));
                return true;
            }

            // SET / GIVE / WITHDRAW
            if (sub.equals("admin") && args.length >= 4) {
                String action = args[1].toLowerCase();
                String targetName = args[2];
                double amount;

                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cSố tiền không hợp lệ!");
                    return true;
                }

                Player target = Bukkit.getPlayer(targetName);

                // --- XỬ LÝ OFFLINE (DATABASE) ---
                if (target == null) {
                    plugin.getDatabaseManager().getUuidByName(targetName).thenAccept(uuid -> {
                        if (uuid == null) {
                            sender.sendMessage("§cKhông tìm thấy người chơi này trong Database!");
                        } else {
                            plugin.getDatabaseManager().loadPlayer(uuid).thenAccept(currentBal -> {

                                // === FIX CẢNH BÁO "REPLACE IF WITH SWITCH" TẠI ĐÂY ===
                                double newBal;
                                switch (action) {
                                    case "set" -> newBal = amount;
                                    case "give" -> newBal = currentBal + amount;
                                    case "withdraw" -> newBal = Math.max(0, currentBal - amount);
                                    default -> {
                                        sender.sendMessage("§cLệnh không tồn tại (chỉ dùng: set, give, withdraw)!");
                                        return; // Dừng lại ngay, không lưu database
                                    }
                                }

                                plugin.getDatabaseManager().savePlayer(uuid, targetName, newBal);
                                sender.sendMessage("§a(Offline) Đã " + action + " " + amount + " Shards cho " + targetName + ". Số dư mới: " + newBal);
                            });
                        }
                    });
                    return true;
                }

                // --- XỬ LÝ ONLINE ---
                switch (action) {
                    case "set" -> shardManager.setBalance(target.getUniqueId(), amount);
                    case "give" -> shardManager.addBalance(target.getUniqueId(), amount);
                    case "withdraw" -> shardManager.withdraw(target.getUniqueId(), amount);
                    default -> {
                        sender.sendMessage("§cLệnh không tồn tại!");
                        return true;
                    }
                }

                String adminMsg = plugin.getConfig().getString("messages.admin-" + action, "&aThao tác thành công!");
                sender.sendMessage(adminMsg.replace("&", "§")
                        .replace("%player%", targetName)
                        .replace("%amount%", String.valueOf(amount)));

                return true;
            }
        } else {
            String noPerm = plugin.getConfig().getString("messages.no-permission", "&cBạn không có quyền!");
            sender.sendMessage(noPerm.replace("&", "§"));
        }

        return true;
    }
}