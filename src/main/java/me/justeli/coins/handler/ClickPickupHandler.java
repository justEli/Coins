package me.justeli.coins.handler;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;

/**
 * Handler for click-based coin pickup
 * -------------------- by AllFiRE
 */
public final class ClickPickupHandler implements Listener {
    private final Coins coins;

    public ClickPickupHandler(Coins coins) {
        this.coins = coins;
        coins.parseEventHandlers(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!Config.CLICK_PICKUP_ENABLED) {
            return;
        }

        Player player = event.getPlayer();

        boolean isCorrectClick = switch (Config.CLICK_PICKUP_BUTTON.toUpperCase()) {
            case "RIGHT" -> event.getAction().name().contains("RIGHT_CLICK");
            case "LEFT" -> event.getAction().name().contains("LEFT_CLICK");
            default -> false;
        };

        if (!isCorrectClick) {
            return;
        }

        Item targetItem = getTargetItem(player);
        if (targetItem == null || !coins.getCoinMeta().isCoin(targetItem.getItemStack())) {
            return;
        }

        event.setCancelled(true);

        double amount = coins.getCoinMeta().getValue(targetItem.getItemStack());
        if (amount == 0) {
            coins.getPickupHandler().depositRandomMoney(targetItem.getItemStack(), player);
        } else {
            coins.getPickupHandler().depositMoney(player, amount);
        }
        targetItem.remove();

        if (Config.PICKUP_SOUND) {
            player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                1.0f,
                1.0f
            );
        }
    }

    private Item getTargetItem(Player player) {
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            5.0,
            entity -> entity instanceof Item
        );

        if (rayTrace != null && rayTrace.getHitEntity() instanceof Item item) {
            return item;
        }
        return null;
    }
}
