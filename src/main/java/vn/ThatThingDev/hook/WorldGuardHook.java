package vn.ThatThingDev.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldGuardHook {

    public static boolean isInRegion(Player player, List<String> allowedRegions) {
        // Logic tối ưu: Check null từ đầu
        if (allowedRegions == null || allowedRegions.isEmpty()) return false;

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return false;

        // Bọc Try-Catch để chống crash nếu WorldGuard lỗi
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));

            if (regions == null) return false;

            ApplicableRegionSet set = regions.getApplicableRegions(BukkitAdapter.asBlockVector(loc));

            for (ProtectedRegion region : set) {
                if (allowedRegions.contains(region.getId())) {
                    return true;
                }
            }
        } catch (Throwable t) {
            // Chỉ in lỗi 1 lần vào console nếu cần, ở đây ta nuốt lỗi để server ko spam
            return false;
        }

        return false;
    }
}