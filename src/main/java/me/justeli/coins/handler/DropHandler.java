package me.justeli.coins.handler;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.item.CoinMeta;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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
    }

    private static final SplittableRandom RANDOM = new SplittableRandom();

    @EventHandler(priority = EventPriority.HIGH)
    void onEntityDeathEvent(EntityDeathEvent event) {
        if (coins.isDisabled()) {
            return;
        }

        LivingEntity dead = event.getEntity();
        if (Util.isDisabledHere(dead.getWorld())) {
            return;
        }

        if (coins.mmHook().isPresent() && Config.DISABLE_MYTHIC_MOB_HANDLING && coins.mmHook().get().isMythicMob(dead)) {
            return;
        }

        if (Config.LOSE_ON_DEATH && dead instanceof Player player && !Permissions.hasBypassLoseOnDeath(player)) {
            handleLosingOnDeath(player);
        }

        if (Config.DROP_WITH_ANY_DEATH) {
            handleDropCheck(dead, null);
            return;
        }

        AttributeInstance maxHealth = dead.getAttribute(Attribute.GENERIC_MAX_HEALTH);
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

        handleDropCheck(dead, attacker.get());
    }

    private void handleLosingOnDeath(@NotNull Player dead) {
        double random = Util.getRandomTakeAmount();
        coins.getEconomy().balance(dead.getUniqueId(), balance -> {
            if (balance <= 0) {
                return;
            }

            double take = Util.round(Config.TAKE_PERCENTAGE? (random / 100) * balance : random);
            if (take <= 0) {
                return;
            }

            coins.getEconomy().withdraw(dead.getUniqueId(), take, () -> {
                Util.sendMessage(dead, Config.DEATH_MESSAGE, Config.DEATH_MESSAGE_POSITION, take);
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

    private void handleDropCheck(@NotNull Entity dead, @Nullable Player attacker) {
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
            handleDrop(dead, attacker);
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

        handleDrop(dead, attacker);
    }

    private void handleDrop(@NotNull Entity dead, @Nullable Player attacker) {
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

        int multiplier = Config.MOB_MULTIPLIER.getOrDefault(dead.getType(), 1);
        dropCoins(multiplier, attacker, dead.getLocation(), false);
    }

    private final Map<Location, Integer> locationAmountCache = new ConcurrentHashMap<>();
    private final Map<Location, Long> locationLastTimeCache = new ConcurrentHashMap<>();

    private boolean isLocationAvailableAndSet(Entity dead) {
        if (Config.LIMIT_FOR_LOCATION < 1) {
            return true;
        }

        Location location = dead.getLocation().getBlock().getLocation();
        long currentTime = System.currentTimeMillis();
        long timeLimit = (long) (3600000 * Config.LOCATION_LIMIT_HOURS);

        // Clean up old entries periodically to prevent memory leaks
        if (this.locationLastTimeCache.size() > 1000) {
            this.locationLastTimeCache.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > timeLimit);
            this.locationAmountCache.entrySet().removeIf(entry -> 
                !this.locationLastTimeCache.containsKey(entry.getKey()));
        }

        long previousTime = this.locationLastTimeCache.computeIfAbsent(location, empty -> 0L);

        if (previousTime > currentTime - timeLimit) {
            int killAmount = this.locationAmountCache.computeIfAbsent(location, empty -> 0);
            this.locationAmountCache.put(location, killAmount + 1);
            this.locationLastTimeCache.put(location, currentTime);
            return killAmount < Config.LIMIT_FOR_LOCATION;
        }

        this.locationAmountCache.put(location, 1);
        this.locationLastTimeCache.put(location, currentTime);
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
            Enchantment enchant = block? Enchantment.LOOT_BONUS_BLOCKS : Enchantment.LOOT_BONUS_MOBS;

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
    void onPlayerJoinEvent(PlayerJoinEvent event) {
        coins.getSettings().resetMultiplier(event.getPlayer());
    }

    public void clearLocationCache() {
        int oldSize = this.locationAmountCache.size();
        this.locationAmountCache.clear();
        this.locationLastTimeCache.clear();
        if (Config.DEBUG_LOGGING) {
            coins.console(Level.INFO, "[Coins] Location cache cleared - removed " + oldSize + " entries");
        }
    }

    public int getLocationCacheSize() {
        return this.locationAmountCache.size();
    }
}
