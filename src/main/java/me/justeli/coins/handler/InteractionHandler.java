package me.justeli.coins.handler;

import me.justeli.coins.Coins;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * @author Eli
 * @since February 4, 2017 (creation)
 */
public final class InteractionHandler implements Listener {
    private final Coins coins;
    public InteractionHandler(Coins coins) {
        this.coins = coins;
        coins.parseEventHandlers(this);
    }

    @EventHandler
    void onPlayerInteractEvent(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            return;
        }

        if (!coins.getCoinMeta().isWithdrawnCoin(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();

        // because of .setAmount(0) AND Container, players have to drop coin instead
        if (!Permissions.hasWithdraw(player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof Container) {
            return;
        }

        event.setCancelled(true);
        if (event.getItem() == null) {
            return;
        }

        double amount = coins.getCoinMeta().getValue(event.getItem());
        event.getItem().setAmount(0);

        coins.getPickupHandler().depositMoney(player, amount);
        Util.playCoinPickupSound(player);
    }
}
