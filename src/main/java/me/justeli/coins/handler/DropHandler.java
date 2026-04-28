package me.justeli.coins.handler;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.event.EntityCoinDropEvent;
import me.justeli.coins.item.CoinMeta;
import me.justeli.coins.util.BlockCache;
import me.justeli.coins.util.BlockPosition;
import me.justeli.coins.component.ComponentUtil;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Eli
 * @since December 13, 2016 (creation)
 */
public final class DropHandler implements Listener {
    private final Coins coins;
    private final NamespacedKey playerDamageKey;

    public DropHandler(Coins coins) {
        this.coins = coins;
        this.playerDamageKey = new NamespacedKey(coins, "coins-player-damage");
        coins.parseEventHandlers(this);

        // clean up unused cache
        SCHEDULED_THREAD.scheduleAtFixedRate(() -> {
            locationLimitCache.entrySet().removeIf(entry -> !entry.getValue().isWithinConfiguredTime());
        }, 10, 10, TimeUnit.MINUTES);
    }

    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static final ScheduledExecutorService SCHEDULED_THREAD =
        Executors.newSingleThreadScheduledExecutor();

    private final Map<BlockPosition, BlockCache> locationLimitCache = new ConcurrentHashMap<>();

    // changed from minecraft:generic.max_health to minecraft:max_health in Minecraft 1.21.4
    private static Attribute MAX_HEALTH_ATTRIBUTE;
    static {
        var key = NamespacedKey.fromString("minecraft:max_health");
        if (key != null) {
            MAX_HEALTH_ATTRIBUTE = Registry.ATTRIBUTE.get(key);
        }

        if (MAX_HEALTH_ATTRIBUTE == null) {
            key = NamespacedKey.fromString("minecraft:generic.max_health");
            if (key != null) {
                MAX_HEALTH_ATTRIBUTE = Registry.ATTRIBUTE.get(key);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    void onEntityDeathEvent(EntityDeathEvent event) {
        if (coins.isDisabled()) {
            return;
        }

        LivingEntity dead = event.getEntity();
        if (Util.isDisabledHere(dead.getWorld())) {
            return;
        }

        if (Config.LOSE_ON_DEATH && dead instanceof Player player && !Permissions.hasBypassLoseOnDeath(player)) {
            handleLosingOnDeath(player);
        }

        if (Config.DROP_WITH_ANY_DEATH) {
            handleEntityCheck(dead, null);
            return;
        }

        AttributeInstance maxHealth = dead.getAttribute(MAX_HEALTH_ATTRIBUTE);
        if (maxHealth == null) {
            return;
        }

        Optional<Player> attacker = Util.getRootDamage(dead);
        if (attacker.isEmpty()) {
            return;
        }

        double percentage = getPlayerDamage(dead) / maxHealth.getValue();
        if (Config.PERCENTAGE_PLAYER_HIT > 0 && percentage < Config.PERCENTAGE_PLAYER_HIT) {
            if (!Permissions.hasBypassPercentageHit(attacker.get())) {
                return;
            }
        }

        handleEntityCheck(dead, attacker.get());
    }

    private void handleLosingOnDeath(@NotNull Player dead) {
        double random = Util.getRandomTakeAmount();
        coins.getEconomy().balance(dead.getUniqueId(), balance -> {
            if (balance <= 0) {
                return;
            }

            double take = Util.toRoundedMoneyDecimals(Config.TAKE_PERCENTAGE? (random / 100) * balance : random);
            if (take <= 0) {
                return;
            }

            coins.getEconomy().withdraw(dead.getUniqueId(), take, () -> {
                coins.getMessenger().sendMessage(
                    dead, Config.DEATH_MESSAGE_POSITION,
                    ComponentUtil.replaceAmount(Config.DEATH_MESSAGE, take)
                );

                if (Config.DROP_ON_DEATH && dead.getLocation().getWorld() != null) {
                    // works on Folia
                    dead.getWorld().dropItem(
                        dead.getLocation(),
                        coins.getCreateCoin().createOther().setData(CoinMeta.COINS_WORTH, take).build()
                    );
                }
            });
        });
    }

    private void handleEntityCheck(@NotNull Entity dead, @Nullable Player attacker) {
        if (Config.PREVENT_SPLITS && coins.getUnfairMobHandler().isFromSplit(dead)) {
            if (attacker == null || !Permissions.hasDropSplitMobs(attacker)) {
                return;
            }
        }

        if (!Config.SPAWNER_DROP && coins.getUnfairMobHandler().isFromSpawner(dead)) {
            if (attacker == null || !Permissions.hasDropSpawnerMobs(attacker)) {
                return;
            }
        }

        if (Config.MOB_MULTIPLIER.containsKey(dead.getType()) && !(dead instanceof Player)) {
            handleEntityCoinDrop(dead, attacker);
            return;
        }

        boolean isHostile = Util.isHostile(dead);
        boolean isPassive = Util.isPassive(dead);
        boolean isPlayer = dead instanceof Player;

        // if none of the possible categories
        if (!isHostile && !isPassive && !isPlayer) {
            return;
        }

        if (!Config.HOSTILE_DROP && isHostile && !Permissions.hasDropHostileMobs(attacker)) {
            return;
        }

        if (!Config.PASSIVE_DROP && isPassive && !Permissions.hasDropPassiveMobs(attacker)) {
            return;
        }

        if (!Config.PLAYER_DROP && isPlayer && !Permissions.hasDropPlayers(attacker)) {
            return;
        }

        handleEntityCoinDrop(dead, attacker);
    }

    private void handleEntityCoinDrop(@NotNull Entity dead, @Nullable Player attacker) {
        if (Config.PREVENT_ALTS && attacker != null && dead instanceof Player victim) {
            var a1 = attacker.getAddress();
            var a2 = victim.getAddress();
            if (a1 != null && a2 != null && a1.getAddress().getHostAddress().equals(a2.getAddress().getHostAddress())) {
                return;
            }
        }

        if (RANDOM.nextDouble() > Config.DROP_CHANCE) {
            return;
        }

        if (!isLocationAvailableAndSet(dead)) {
            if (attacker == null || !Permissions.hasBypassLocationLimit(attacker)) {
                return;
            }
        }

        EntityCoinDropEvent registerEvent = new EntityCoinDropEvent(attacker, dead);
        coins.getServer().getPluginManager().callEvent(registerEvent);
        if (registerEvent.isCancelled()) {
            return;
        }

        int multiplier = Config.MOB_MULTIPLIER.getOrDefault(dead.getType(), 1);
        dropCoins(multiplier, attacker, dead.getLocation(), false);
    }

    private boolean isLocationAvailableAndSet(Entity dead) {
        if (Config.LIMIT_FOR_LOCATION < 1) {
            return true;
        }

        BlockPosition position = new BlockPosition(dead.getLocation());
        BlockCache cache = locationLimitCache.computeIfAbsent(position, empty -> new BlockCache());

        if (cache.isWithinConfiguredTime()) {
            return cache.getAndIncrement() < Config.LIMIT_FOR_LOCATION;
        }

        cache.getAndIncrement();
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onBlockBreakEvent(BlockBreakEvent event) {
        if (coins.isDisabled() || Util.isDisabledHere(event.getBlock().getWorld())) {
            return;
        }

        if (Config.MINE_PERCENTAGE == 0) {
            return;
        }

        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL || isBlockDropSameItem(event)) {
            return;
        }

        int multiplier = Config.BLOCK_DROPS.computeIfAbsent(event.getBlock().getType(), empty -> 0);
        if (multiplier == 0) {
            return;
        }

        if (RANDOM.nextDouble() > Config.MINE_PERCENTAGE) {
            return;
        }

        dropCoins(multiplier, event.getPlayer(), event.getBlock().getLocation().add(.5, .5, .5), true);
    }

    // if the block that is mined is exactly the same as the items it drops
    private boolean isBlockDropSameItem(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        for (ItemStack item : event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
            if (item.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private void dropCoins(int amount, @Nullable Player player, @NotNull Location location, boolean block) {
        if (location.getWorld() == null) {
            return;
        }

        double increment = 1;
        if (player != null && Config.ENCHANT_INCREMENT > 0) {
            Enchantment enchant = block? Enchantment.FORTUNE : Enchantment.LOOTING;

            int lootingLevel = player.getInventory().getItemInMainHand().getEnchantmentLevel(enchant);
            if (lootingLevel > 0) {
                increment += lootingLevel * Config.ENCHANT_INCREMENT;
            }
        }

        if (Config.DROP_EACH_COIN) {
            amount *= (int) ((Util.getRandomMoneyAmount() + .5) * increment);
            increment = 1;
        }

        if (player != null) {
            amount *= (int) coins.getSettings().getMultiplier(player);
        }

        for (int i = 0; i < amount; i++) {
            // works on Folia
            ItemStack coin = coins.getCreateCoin().createDropped(increment);
            if (block) {
                coins.getScheduler().runLocationTaskLater(location, 1, () ->
                    location.getWorld().dropItemNaturally(location, coin)
                );
            }
            else {
                location.getWorld().dropItem(location, coin);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (Util.getRootDamage(event).isEmpty()) {
            return;
        }

        double playerDamage = getPlayerDamage(event.getEntity());
        event.getEntity().getPersistentDataContainer().set(
            playerDamageKey,
            PersistentDataType.DOUBLE,
            playerDamage + event.getFinalDamage()
        );
    }

    private double getPlayerDamage(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().getOrDefault(playerDamageKey, PersistentDataType.DOUBLE, 0D);
    }

    @EventHandler
    void onPlayerQuitEvent(PlayerQuitEvent event) {
        coins.getSettings().resetMultiplier(event.getPlayer());
    }
}
