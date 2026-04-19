package me.justeli.coins.util;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Eli
 * @since April 19, 2026
 */
public final class BlockPosition {
    private final @NotNull UUID worldUuid;
    private final int x, y, z;

    public BlockPosition(@NotNull Location location) {
        this.worldUuid = location.getWorld().getUID();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockPosition position) {
            return this.worldUuid.equals(position.worldUuid)
                && this.x == position.x
                && this.y == position.y
                && this.z == position.z;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldUuid, x, y, z);
    }
}
