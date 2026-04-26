package me.justeli.coins.handler.listener;

import me.justeli.coins.Coins;
import me.justeli.coins.event.PickupEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.logging.Level;

/**
 * @author Eli
 * @since September 13, 2020 (creation)
 */
public final class SpigotEventListener implements Listener {
    private final Coins coins;
    public SpigotEventListener(Coins coins) {
        this.coins = coins;
        coins.parseEventHandlers(this);
        coins.console(Level.WARNING, "Players with a full inventory can pick up coins when using Paper server software.");
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityPickupItemEvent(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PickupEvent registerEvent = new PickupEvent(player, event.getItem());
        coins.getServer().getPluginManager().callEvent(registerEvent);

        if (registerEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }
}
