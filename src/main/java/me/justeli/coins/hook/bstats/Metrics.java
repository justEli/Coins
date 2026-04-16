package me.justeli.coins.hook.bstats;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.config.Settings;
import me.justeli.coins.util.VersionUtil;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

/**
 * @author Eli
 * @since July 09, 2021 (creation)
 */
public final class Metrics {
    private final Coins coins;
    public Metrics(Coins coins) {
        this.coins = coins;
        register();
    }

    public static void metrics(JavaPlugin plugin, final Consumer<Metric> consumer) {
        org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(plugin, 831);
        consumer.accept(new Metric(metrics));
    }

    public static class Metric {
        private final org.bstats.bukkit.Metrics metrics;

        public Metric(org.bstats.bukkit.Metrics metrics) {
            this.metrics = metrics;
        }

        public void add(String key, Object value) {
            if (value == null || value.toString() == null) {
                return;
            }

            metrics.addCustomChart(new SimplePie(key, value::toString));
        }
    }

    public void register() {
        metrics(coins, metrics -> {
            metrics.add("language", Config.LANGUAGE.toLowerCase());
            metrics.add("currencySymbol", Config.CURRENCY_SYMBOL);
            metrics.add("coinItem", Config.COIN_ITEM);
            metrics.add("enchantedCoin", Config.ENCHANTED_COIN);
            metrics.add("pickupMessage", Config.PICKUP_MESSAGE);
            metrics.add("dropEachCoin", Config.DROP_EACH_COIN);
            metrics.add("dropWithAnyDeath", Config.DROP_WITH_ANY_DEATH);
            metrics.add("moneyAmount", (Config.MONEY_AMOUNT_FROM + Config.MONEY_AMOUNT_TO) / 2);
            metrics.add("moneyDecimals", Config.MONEY_DECIMALS);
            metrics.add("stackCoins", Config.STACK_COINS);
            metrics.add("percentagePlayerHit", Config.PERCENTAGE_PLAYER_HIT * 100 + "%");
            metrics.add("disableHoppers", Config.DISABLE_HOPPERS);
            metrics.add("playerDrop", Config.PLAYER_DROP);
            metrics.add("preventAlts", Config.PREVENT_ALTS);
            metrics.add("spawnerDrop", Config.SPAWNER_DROP);
            metrics.add("passiveDrop", Config.PASSIVE_DROP);
            metrics.add("preventSplits", Config.PREVENT_SPLITS);
            metrics.add("soundEnabled", Config.PICKUP_SOUND);
            metrics.add("pickupSound", Config.SOUND_NAME);
            metrics.add("soundPitch", Config.SOUND_PITCH);
            metrics.add("soundVolume", Config.SOUND_VOLUME);
            metrics.add("dropChance", Config.DROP_CHANCE * 100 + "%");
            metrics.add("limitForLocation", Config.LIMIT_FOR_LOCATION);
            metrics.add("customModelData", Config.CUSTOM_MODEL_DATA);
            metrics.add("enableWithdraw", Config.ENABLE_WITHDRAW);
            metrics.add("maxWithdrawAmount", Config.MAX_WITHDRAW_AMOUNT);
            metrics.add("minePercentage", Config.MINE_PERCENTAGE * 100 + "%");
            metrics.add("hostile-drop", Config.HOSTILE_DROP);
            metrics.add("loseOnDeath", Config.LOSE_ON_DEATH);
            metrics.add("moneyTaken", (Config.MONEY_TAKEN_FROM + Config.MONEY_TAKEN_TO) / 2);
            metrics.add("takePercentage", Config.TAKE_PERCENTAGE);
            metrics.add("dropOnDeath", Config.DROP_ON_DEATH);
            metrics.add("deathMessage", Config.DEATH_MESSAGE);
            metrics.add("location-limit-hours", Config.LOCATION_LIMIT_HOURS);
            metrics.add("usingSkullTexture", Config.SKULL_TEXTURE != null && !Config.SKULL_TEXTURE.isEmpty());
            metrics.add("usingPaper", VersionUtil.isPlatformAtLeast(VersionUtil.Platform.PAPER));
            metrics.add("platform", VersionUtil.getPlatform().getName()); // todo add to bstats page
            metrics.add("usingMythicMobs", coins.mmHook().isPresent());
            metrics.add("droppedCoinName", Config.DROPPED_COIN_NAME);
            metrics.add("withdrawnCoinNamesSingular", Config.WITHDRAWN_COIN_NAME_SINGULAR);
            metrics.add("withdrawnCoinNamesPlural", Config.WITHDRAWN_COIN_NAME_PLURAL);
            metrics.add("detectLegacyCoins", false);
            metrics.add("allowNameChange", Config.ALLOW_NAME_CHANGE);
            metrics.add("allowModification", Config.ALLOW_MODIFICATION);
            metrics.add("checkForUpdates", Config.CHECK_FOR_UPDATES);
            metrics.add("enchantIncrement", Config.ENCHANT_INCREMENT);
            metrics.add("usingLegacyKeysNew", Settings.USING_LEGACY_KEYS);
            metrics.add("economyHook", coins.getEconomy().name().orElse("None"));

            Plugin mm = coins.getServer().getPluginManager().getPlugin("MythicMobs");
            if (mm != null) {
                metrics.add("mythicMobsVersion", mm.getDescription().getVersion());
            }
        });
    }
}
