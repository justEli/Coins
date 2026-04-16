package me.justeli.coins.util;

import me.justeli.coins.Coins;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Eli
 * @since April 16, 2026
 */
public final class ScheduleUtil {
    private final Coins coins;
    public ScheduleUtil(Coins coins) {
        this.coins = coins;
    }

    public void runEntityTaskLater(Entity entity, long ticks, Runnable runnable) {
        if (VersionUtil.getPlatform() == VersionUtil.Platform.FOLIA) {
            entity.getScheduler().runDelayed(coins, task -> runnable.run(), runnable, ticks);
        }
        else {
            coins.getServer().getScheduler().runTaskLater(coins, runnable, ticks);
        }
    }

    public void runLocationTaskLater(Location location, long ticks, Runnable runnable) {
        if (VersionUtil.getPlatform() == VersionUtil.Platform.FOLIA) {
            coins.getServer().getRegionScheduler().runDelayed(coins, location, task -> runnable.run(), ticks);
        }
        else {
            coins.getServer().getScheduler().runTaskLater(coins, runnable, ticks);
        }
    }

    public void runLocationTaskRepeated(Location location, long amount, long period, Runnable runnable) {
        if (VersionUtil.getPlatform() == VersionUtil.Platform.FOLIA) {
            AtomicInteger ticks = new AtomicInteger();
            coins.getServer().getRegionScheduler().runAtFixedRate(coins, location, (task) -> {
                runnable.run();

                if (ticks.addAndGet(1) >= amount) {
                    task.cancel();
                }
            }, 1, period);
        }
        else {
            new BukkitRunnable() {
                private final AtomicInteger ticks = new AtomicInteger();

                @Override
                public void run() {
                    runnable.run();

                    if (ticks.addAndGet(1) >= amount) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(coins, 0, period);
        }
    }
}
