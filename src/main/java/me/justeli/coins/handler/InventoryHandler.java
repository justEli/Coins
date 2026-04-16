package me.justeli.coins.handler;

import me.justeli.coins.Coins;
import me.justeli.coins.util.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * @author Eli
 * @since May 2, 2019 (creation)
 */
public final class InventoryHandler implements Listener {
    private final Coins coins;
    public InventoryHandler(Coins coins) {
        this.coins = coins;
        coins.parseEventHandlers(this);
    }

    // todo maybe this should be removed, or a config option
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onInventoryClickEvent(InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (Util.isDisabledHere(event.getWhoClicked().getWorld())) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!coins.getCoinMeta().isDroppedCoin(event.getCurrentItem())) {
            return;
        }

        event.setCancelled(true);
        coins.getPickupHandler().depositRandomMoney(event.getCurrentItem(), player);
        event.getCurrentItem().setAmount(0);
    }
}
