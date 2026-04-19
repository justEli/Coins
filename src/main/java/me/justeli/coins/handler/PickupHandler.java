package me.justeli.coins.handler;

import me.justeli.coins.Coins;
import me.justeli.coins.event.PickupEvent;
import me.justeli.coins.config.Config;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eli
 * @since December 13, 2016 (creation)
 */
public final class PickupHandler implements Listener {
    private final Coins coins;
    public PickupHandler(Coins coins) {
        this.coins = coins;
        coins.parseEventHandlers(this);
    }

    private final Map<UUID, Double> pickupAmountCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pickupTimeCache = new ConcurrentHashMap<>();

    private static final Vector THROW_VECTOR = new Vector(0, .4, 0);
    private static final UUID NO_OWNER_UUID = UUID.fromString("00000001-0001-0001-0001-0000000000AD");

    @EventHandler(ignoreCancelled = true)
    void onPickupEvent(PickupEvent event) {
        if (Util.isDisabledHere(event.getPlayer().getWorld())) {
            return;
        }

        Item item = event.getItem();
        if (!coins.getCoinMeta().isCoin(item.getItemStack())) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (Permissions.hasCoinsDisabled(player)) {
            return;
        }

        // don't let players pick up coins that are already being picked up
        if (item.getPickupDelay() > 0) {
            return;
        }

        // prevent pickup while coin is thrown up
        item.setPickupDelay(1200);
        item.setOwner(NO_OWNER_UUID); // prevent the item from pickup in the future

        // throw the coin upwards
        item.setVelocity(THROW_VECTOR);
        coins.getScheduler().runEntityTaskLater(item, 5, item::remove);

        // give money for picking up
        double amount = coins.getCoinMeta().getValue(item.getItemStack());
        if (amount == 0) {
            depositRandomMoney(item.getItemStack(), player);
        }
        else {
            depositMoney(player, amount);
        }

        if (Config.PICKUP_SOUND) {
            Util.playCoinPickupSound(player);
        }
    }

    @EventHandler
    void onEntityPickupItemEvent(EntityPickupItemEvent event) {
        // prevention to pick up is already handled properly at PickupEvent
        if (event.getEntity() instanceof Player) {
            return;
        }

        // don't let mobs pick up coins that are already being picked up by players
        // only canceled when the pickup delay was set (to prevent double pickup)
        Item item = event.getItem();
        if (item.getPickupDelay() == 0 || !coins.getCoinMeta().isCoin(item.getItemStack())) {
            return;
        }

        event.setCancelled(true);
    }

    public void depositRandomMoney(ItemStack item, Player player) {
        if (Config.DROP_EACH_COIN) {
            depositMoney(player, item.getAmount());
            return;
        }

        depositMoney(player, item.getAmount() * Util.getRandomMoneyAmount() * coins.getCoinMeta().getIncrement(item));
    }

    public void depositMoney(Player player, double amount) {
        double rounded = Util.round(amount);
        coins.getEconomy().deposit(player.getUniqueId(), rounded, () -> {
            UUID uuid = player.getUniqueId();
            long previousTime = pickupTimeCache.computeIfAbsent(uuid, empty -> 0L);

            if (previousTime > System.currentTimeMillis() - 1500) {
                // recently shown actionbar
                double previousAmount = pickupAmountCache.computeIfAbsent(uuid, empty -> 0D);
                pickupAmountCache.put(uuid, rounded + previousAmount);
            }
            else {
                pickupAmountCache.put(uuid, rounded);
            }

            double displayAmount = pickupAmountCache.computeIfAbsent(uuid, empty -> 0D);
            if (!Config.PICKUP_MESSAGE.isEmpty()) {
                Util.sendMessage(player, Config.PICKUP_MESSAGE, Config.PICKUP_MESSAGE_POSITION, displayAmount);
            }

            pickupTimeCache.put(uuid, System.currentTimeMillis());
        });
    }

    @EventHandler
    void onPlayerQuitEvent(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        pickupAmountCache.remove(uuid);
        pickupTimeCache.remove(uuid);
    }
}
