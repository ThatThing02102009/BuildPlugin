package vn.ThatThingDev.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import vn.ThatThingDev.Main;

public class ShardPlaceholder extends PlaceholderExpansion {
    private final Main plugin;

    public ShardPlaceholder(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "shards"; }

    @Override
    public @NotNull String getAuthor() { return "ThatThingDev"; }

    @Override
    public @NotNull String getVersion() { return "1.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player != null && params.equalsIgnoreCase("value")) {
            return String.valueOf(plugin.getShardManager().getBalance(player.getUniqueId()));
        }
        return null;
    }
}