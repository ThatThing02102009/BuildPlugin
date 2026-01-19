package vn.ThatThingDev.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import vn.ThatThingDev.Main;

import java.text.DecimalFormat;

public class ShardPlaceholder extends PlaceholderExpansion {

    private final Main plugin;
    private final DecimalFormat df = new DecimalFormat("#,###.#");

    public ShardPlaceholder(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "shards"; // Đây là tiền tố: %shards_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "ThatThingDev";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // Giữ hook khi reload PAPI
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // Lấy số dư từ Cache (nhanh) hoặc DB
        double balance = plugin.getShardManager().getBalance(player.getUniqueId());

        if (params.equalsIgnoreCase("balance")) {
            return String.valueOf(balance); // Trả về: 1050.5
        }

        if (params.equalsIgnoreCase("balance_int")) {
            return String.valueOf((int) balance); // Trả về: 1050
        }

        if (params.equalsIgnoreCase("balance_formatted")) {
            return df.format(balance); // Trả về: 1,050.5
        }

        return null;
    }
}