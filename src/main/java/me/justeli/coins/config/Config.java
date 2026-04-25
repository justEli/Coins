package me.justeli.coins.config;

import me.justeli.coins.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eli
 * @since December 14, 2016 (creation); July 9, 2021 (rewrite)
 */
public class Config {
    private Config() {}

    public static Map<Material, Integer> BLOCK_DROPS = new ConcurrentHashMap<>();
    public static Map<EntityType, Integer> MOB_MULTIPLIER = new ConcurrentHashMap<>();

    @ConfigEntry(value = "debug-logging", required = false)
    public static Boolean DEBUG_LOGGING = false;

    @ConfigEntry("stack-coins")
    public static Boolean STACK_COINS = false;

    @ConfigEntry("spawner-drop")
    public static Boolean SPAWNER_DROP = false;

    @ConfigEntry("passive-drop")
    public static Boolean PASSIVE_DROP = false;

    @ConfigEntry("hostile-drop")
    public static Boolean HOSTILE_DROP = true;

    @ConfigEntry("pickup-sound")
    public static Boolean PICKUP_SOUND = true;

    @ConfigEntry("lose-on-death")
    public static Boolean LOSE_ON_DEATH = true;

    @ConfigEntry("player-drop")
    public static Boolean PLAYER_DROP = true;

    @ConfigEntry("prevent-alts")
    public static Boolean PREVENT_ALTS = true;

    @ConfigEntry("enable-withdraw")
    public static Boolean ENABLE_WITHDRAW = true;

    @ConfigEntry("drop-each-coin")
    public static Boolean DROP_EACH_COIN = false;

    @ConfigEntry("prevent-splits")
    public static Boolean PREVENT_SPLITS = true;

    @ConfigEntry("take-percentage")
    public static Boolean TAKE_PERCENTAGE = false;

    @ConfigEntry("drop-on-death")
    public static Boolean DROP_ON_DEATH = false;

    @ConfigEntry("disable-hoppers")
    public static Boolean DISABLE_HOPPERS = false;

    @ConfigEntry("drop-with-any-death")
    public static Boolean DROP_WITH_ANY_DEATH = false;

    @ConfigEntry("enchanted-coin")
    public static Boolean ENCHANTED_COIN = false;

    @ConfigEntry("disable-mythic-mob-handling")
    public static Boolean DISABLE_MYTHIC_MOB_HANDLING = false;

    @ConfigEntry("allow-name-change")
    public static Boolean ALLOW_NAME_CHANGE = false;

    @ConfigEntry("allow-modification")
    public static Boolean ALLOW_MODIFICATION = false;

    @ConfigEntry(value = "check-for-updates", required = false)
    public static Boolean CHECK_FOR_UPDATES = true;

    @Deprecated
    @ConfigEntry(value = "language", required = false)
    public static String LANGUAGE = "";

    @ConfigEntry("locale")
    public static String LOCALE = "en-US";

    @ConfigEntry("coin-item")
    public static Material COIN_ITEM = Material.SUNFLOWER;

    @ConfigEntry("pickup-message")
    public static Component PICKUP_MESSAGE = ComponentUtil.parse("<up>↑ <money>{currency}{amount}");

    @ConfigEntry("pickup-message-position")
    public static MessagePosition PICKUP_MESSAGE_POSITION = MessagePosition.ACTIONBAR;

    @ConfigEntry("withdraw-message")
    public static Component WITHDRAW_MESSAGE = ComponentUtil.parse("<down>↓ <money>{currency}{amount}");

    @ConfigEntry("withdraw-message-position")
    public static MessagePosition WITHDRAW_MESSAGE_POSITION = MessagePosition.ACTIONBAR;

    @ConfigEntry("death-message")
    public static Component DEATH_MESSAGE = ComponentUtil.parse("<down>↓ <money>{currency}{amount}");

    @ConfigEntry("death-message-position")
    public static MessagePosition DEATH_MESSAGE_POSITION = MessagePosition.SUBTITLE;

    @ConfigEntry("sound-name")
    public static SoundKey SOUND_NAME = new SoundKey("minecraft:item.armor.equip_gold");

    @ConfigEntry("currency-symbol")
    public static String CURRENCY_SYMBOL = "$";

    @ConfigEntry("skull-texture")
    public static String SKULL_TEXTURE = "";

    @ConfigEntry("digit-decimal-separator")
    public static String DIGIT_DECIMAL_SEPARATOR = ".";

    @ConfigEntry("digit-group-separator")
    public static String DIGIT_GROUP_SEPARATOR = ",";

    @ConfigEntry("dropped-coin-name")
    public static Component DROPPED_COIN_NAME = ComponentUtil.parse("<coins>Coin");

    @ConfigEntry("withdrawn-coin-names.singular")
    public static Component WITHDRAWN_COIN_NAME_SINGULAR = ComponentUtil.parse("<var>{amount} <coins>Coin");

    @ConfigEntry("withdrawn-coin-names.plural")
    public static Component WITHDRAWN_COIN_NAME_PLURAL = ComponentUtil.parse("<var>{amount} <coins>Coins");

    @ConfigEntry("drop-chance")
    public static Double DROP_CHANCE = 0.9;

    @ConfigEntry("max-withdraw-amount")
    public static Double MAX_WITHDRAW_AMOUNT = 10000.0;

    @ConfigEntry("money-amount.from")
    public static Double MONEY_AMOUNT_FROM = 3.0;

    @ConfigEntry("money-amount.to")
    public static Double MONEY_AMOUNT_TO = 7.0;

    @ConfigEntry("money-taken.from")
    public static Double MONEY_TAKEN_FROM = 10.0;

    @ConfigEntry("money-taken.to")
    public static Double MONEY_TAKEN_TO = 30.0;

    @ConfigEntry("mine-percentage")
    public static Double MINE_PERCENTAGE = 0.3;

    @ConfigEntry("percentage-player-hit")
    public static Double PERCENTAGE_PLAYER_HIT = 0.8;

    @ConfigEntry("enchant-increment")
    public static Double ENCHANT_INCREMENT = 0.05;

    @ConfigEntry("location-limit-hours")
    public static Double LOCATION_LIMIT_HOURS = 1.0;

    @ConfigEntry("sound-pitch")
    public static Float SOUND_PITCH = 0.3F;

    @ConfigEntry("sound-volume")
    public static Float SOUND_VOLUME = 0.5F;

    @ConfigEntry("money-decimals")
    public static Integer MONEY_DECIMALS = 2;

    @ConfigEntry("limit-for-location")
    public static Integer LIMIT_FOR_LOCATION = 1;

    @ConfigEntry("custom-model-data")
    public static Integer CUSTOM_MODEL_DATA = 0;

    @ConfigEntry("disabled-worlds")
    public static Set<String> DISABLED_WORLDS = new HashSet<>();

    @ConfigEntry("mob-multiplier")
    protected static Map<String, Integer> RAW_MOB_MULTIPLIER = new HashMap<>();

    @ConfigEntry("block-drops")
    protected static Map<String, Integer> RAW_BLOCK_DROPS = new HashMap<>();
}
