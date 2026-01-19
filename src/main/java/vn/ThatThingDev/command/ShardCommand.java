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
        if (args.length == 0) {
            sender.sendMessage("§e--------- AfkShards v2.2 ---------");
            sender.sendMessage("§6/shards bal");
            if (sender.hasPermission("shards.admin")) {
                sender.sendMessage("§c/shards admin reload");
                sender.sendMessage("§c/shards admin set/give/withdraw <player> <amount>");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("bal") || sub.equals("balance")) {
            if (sender instanceof Player p) {
                double bal = shardManager.getBalance(p.getUniqueId());
                sender.sendMessage(plugin.getConfig().getString("messages.balance", "&e%amount%")
                        .replace("&", "§").replace("%amount%", String.valueOf(bal)));
            }
            return true;
        }

        if (sender.hasPermission("shards.admin")) {
            if (sub.equals("reload")) {
                plugin.reloadConfig();
                sender.sendMessage(plugin.getConfig().getString("messages.reload", "&aReloaded!").replace("&", "§"));
                return true;
            }

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

                // Logic Online
                if (target != null) {
                    switch (action) {
                        case "set" -> shardManager.setBalance(target.getUniqueId(), amount);
                        case "give" -> shardManager.addBalance(target.getUniqueId(), amount);
                        case "withdraw" -> shardManager.withdraw(target.getUniqueId(), amount);
                    }
                    sender.sendMessage("§aDone (Online): " + targetName + " -> " + amount);
                    return true;
                }

                // Logic Offline (Callback Hell solver)
                plugin.getDatabaseManager().getUuidByName(targetName).thenAccept(uuid -> {
                    if (uuid == null) {
                        sender.sendMessage("§cPlayer not found!");
                        return;
                    }
                    plugin.getDatabaseManager().loadPlayer(uuid).thenAccept(current -> {
                        double newVal = switch (action) {
                            case "set" -> amount;
                            case "give" -> current + amount;
                            case "withdraw" -> Math.max(0, current - amount);
                            default -> current;
                        };
                        plugin.getDatabaseManager().savePlayer(uuid, targetName, newVal);
                        sender.sendMessage("§aDone (Offline): " + targetName + " -> " + newVal);
                    });
                });
                return true;
            }
        }
        return true;
    }
}