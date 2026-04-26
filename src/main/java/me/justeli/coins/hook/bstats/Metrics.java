package me.justeli.coins.hook.bstats;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.config.Settings;
import me.justeli.coins.util.ComponentUtil;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * @author Eli
 * @since July 09, 2021 (creation)
 */
public final class Metrics {
    private final Coins coins;
    public Metrics(Coins coins) {
        this.coins = coins;
    }

    public static void metrics(JavaPlugin plugin, Consumer<Metric> consumer) {
        consumer.accept(new Metric(new org.bstats.bukkit.Metrics(plugin, 831)));
    }

    public static class Metric {
        private final org.bstats.bukkit.Metrics metrics;

        public Metric(org.bstats.bukkit.Metrics metrics) {
            this.metrics = metrics;
        }

        public void add(String key, @NotNull Callable<Object> callable) {
            metrics.addCustomChart(new SimplePie(key, () -> callable.call().toString()));
        }
    }

    private static String toRounded(double value) {
        return "%.2f".formatted(value);
    }

    private static String toPercentage(double amount) {
        return (int) Math.clamp(amount * 100D, 0, 100) + "%";
    }

    private static final String ECONOMY_HOOK = "economyHook";

    public void register(boolean empty) {
        if (empty) {
            metrics(coins, metrics ->
                metrics.add(ECONOMY_HOOK, () -> coins.getEconomy().name().orElse("None"))
            );
            return;
        }

        metrics(coins, metrics -> {
            metrics.add("locale", () -> Config.LOCALE);
            metrics.add("currencySymbol", () -> Config.CURRENCY_SYMBOL);
            metrics.add("coinItem", () -> coins.getBaseCoin().cloneBaseDropped().build().getType().getKey().toString());
            metrics.add("usingSkullTexture", () -> Config.SKULL_TEXTURE != null && !Config.SKULL_TEXTURE.isEmpty());
            metrics.add("enchantedCoin", () -> Config.ENCHANTED_COIN);
            metrics.add("pickupMessage", () -> ComponentUtil.toStripped(Config.PICKUP_MESSAGE));
            metrics.add("withdrawMessage", () -> ComponentUtil.toStripped(Config.WITHDRAW_MESSAGE));
            metrics.add("dropEachCoin", () -> Config.DROP_EACH_COIN);
            metrics.add("dropWithAnyDeath", () -> Config.DROP_WITH_ANY_DEATH);
            metrics.add("moneyAmount", () -> toRounded((Config.MONEY_AMOUNT_FROM + Config.MONEY_AMOUNT_TO) / 2D));
            metrics.add("moneyDecimals", () -> Config.MONEY_DECIMALS);
            metrics.add("stackCoins", () -> Config.STACK_COINS);
            metrics.add("percentagePlayerHit", () -> toPercentage(Config.PERCENTAGE_PLAYER_HIT));
            metrics.add("disableHoppers", () -> Config.DISABLE_HOPPERS);
            metrics.add("playerDrop", () -> Config.PLAYER_DROP);
            metrics.add("preventAlts", () -> Config.PREVENT_ALTS);
            metrics.add("spawnerDrop", () -> Config.SPAWNER_DROP);
            metrics.add("passiveDrop", () -> Config.PASSIVE_DROP);
            metrics.add("hostileDrop", () -> Config.HOSTILE_DROP);
            metrics.add("preventSplits", () -> Config.PREVENT_SPLITS);
            metrics.add("soundEnabled", () -> Config.PICKUP_SOUND);
            metrics.add("pickupSound", () -> Config.SOUND_NAME);
            metrics.add("soundPitch", () -> toRounded(Config.SOUND_PITCH));
            metrics.add("soundVolume", () -> toRounded(Config.SOUND_VOLUME));
            metrics.add("dropChance", () -> toPercentage(Config.DROP_CHANCE));
            metrics.add("limitForLocation", () -> Config.LIMIT_FOR_LOCATION);
            metrics.add("customModelData", () -> Config.CUSTOM_MODEL_DATA);
            metrics.add("enableWithdraw", () -> Config.ENABLE_WITHDRAW);
            metrics.add("maxWithdrawAmount", () -> "%.0f".formatted(Config.MAX_WITHDRAW_AMOUNT));
            metrics.add("minePercentage", () -> toPercentage(Config.MINE_PERCENTAGE));
            metrics.add("loseOnDeath", () -> Config.LOSE_ON_DEATH);
            metrics.add("moneyTaken", () -> toRounded((Config.MONEY_TAKEN_FROM + Config.MONEY_TAKEN_TO) / 2));
            metrics.add("takePercentage", () -> Config.TAKE_PERCENTAGE);
            metrics.add("dropOnDeath", () -> Config.DROP_ON_DEATH);
            metrics.add("deathMessage", () -> ComponentUtil.toStripped(Config.DEATH_MESSAGE));
            metrics.add("locationLimitHours", () -> toRounded(Config.LOCATION_LIMIT_HOURS));
            metrics.add("droppedCoinName", () -> ComponentUtil.toStripped(Config.DROPPED_COIN_NAME));
            metrics.add("withdrawnCoinNamesSingular", () -> ComponentUtil.toStripped(Config.WITHDRAWN_COIN_NAME_SINGULAR));
            metrics.add("withdrawnCoinNamesPlural", () -> ComponentUtil.toStripped(Config.WITHDRAWN_COIN_NAME_PLURAL));
            metrics.add("allowNameChange", () -> Config.ALLOW_NAME_CHANGE);
            metrics.add("allowModification", () -> Config.ALLOW_MODIFICATION);
            metrics.add("checkForUpdates", () -> Config.CHECK_FOR_UPDATES);
            metrics.add("enchantIncrement", () -> toPercentage(Config.ENCHANT_INCREMENT));
            metrics.add("legacyConfigKeys", () -> Settings.USING_LEGACY_KEYS);
            metrics.add(ECONOMY_HOOK, () -> coins.getEconomy().name().orElse("None"));
            metrics.add("disabledWorldsCount", () -> Config.DISABLED_WORLDS.size());
            metrics.add("mobMultipliers", () -> Config.MOB_MULTIPLIER.size());
            metrics.add("blockDrops", () -> Config.BLOCK_DROPS.size());
            metrics.add("pickupMessagePosition", () -> Config.PICKUP_MESSAGE_POSITION.name().toLowerCase());
            metrics.add("withdrawMessagePosition", () -> Config.WITHDRAW_MESSAGE_POSITION.name().toLowerCase());
            metrics.add("deathMessagePosition", () -> Config.DEATH_MESSAGE_POSITION.name().toLowerCase());
            metrics.add("usingOldColorCodes", () -> Settings.USING_OLD_COLOR_CODES);
            metrics.add("usingOldPlaceholders", () -> Settings.USING_OLD_PLACEHOLDERS);
            metrics.add("migratedToLocale", () -> Settings.MIGRATED_TO_LOCALE);

            // mythicmobs
            metrics.add("usingMythicMobs", () -> coins.getServer().getPluginManager().isPluginEnabled("MythicMobs"));
            var mm = coins.getServer().getPluginManager().getPlugin("MythicMobs");
            if (mm != null) {
                metrics.add("mythicMobsVersion", () -> mm.getDescription().getVersion().split("-")[0]);
            }
        });
    }
}
