package me.justeli.coins.util;

import me.justeli.coins.config.Config;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Eli
 * @since April 19, 2026
 */
public final class BlockCache {
    // both 0 because nothing has happened yet when the object is created
    private final AtomicInteger amount = new AtomicInteger(0);
    private final AtomicLong lastTime = new AtomicLong(0);

    public int getAndIncrement() {
        lastTime.set(System.currentTimeMillis());
        return amount.getAndIncrement();
    }

    public boolean isWithinConfiguredTime() {
        return lastTime.get() > System.currentTimeMillis() - 3600000 * Config.LOCATION_LIMIT_HOURS;
    }
}
