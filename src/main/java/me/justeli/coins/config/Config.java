package me.justeli.coins.config;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.text.DecimalFormat;
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
    public static DecimalFormat DECIMAL_FORMATTER = new DecimalFormat();

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

    @ConfigEntry("language")
    public static String LANGUAGE = "English";

    @ConfigEntry("coin-item")
    public static Material COIN_ITEM = Material.SUNFLOWER;

    @ConfigEntry("pickup-message")
    public static String PICKUP_MESSAGE = "&2+ &a{currency}{amount}";

    @ConfigEntry("pickup-message-position")
    public static MessagePosition PICKUP_MESSAGE_POSITION = MessagePosition.ACTIONBAR;

    @ConfigEntry("withdraw-message")
    public static String WITHDRAW_MESSAGE = "&4- &c{currency}{amount}";

    @ConfigEntry("withdraw-message-position")
    public static MessagePosition WITHDRAW_MESSAGE_POSITION = MessagePosition.ACTIONBAR;

    @ConfigEntry("death-message")
    public static String DEATH_MESSAGE = "&4- &c{currency}{amount}";

    @ConfigEntry("death-message-position")
    public static MessagePosition DEATH_MESSAGE_POSITION = MessagePosition.SUBTITLE;

    @ConfigEntry("sound-name")
    public static Sound SOUND_NAME = Sound.ITEM_ARMOR_EQUIP_GOLD;

    @ConfigEntry("currency-symbol")
    public static String CURRENCY_SYMBOL = "$";

    @ConfigEntry("skull-texture")
    public static String SKULL_TEXTURE = "";

    @ConfigEntry("digit-decimal-separator")
    public static String DIGIT_DECIMAL_SEPARATOR = ".";

    @ConfigEntry("digit-group-separator")
    public static String DIGIT_GROUP_SEPARATOR = ",";

    @ConfigEntry("dropped-coin-name")
    public static String DROPPED_COIN_NAME = "&6Coin";

    @ConfigEntry("withdrawn-coin-names.singular")
    public static String WITHDRAWN_COIN_NAME_SINGULAR = "&e{amount} &6Coin";

    @ConfigEntry("withdrawn-coin-names.plural")
    public static String WITHDRAWN_COIN_NAME_PLURAL = "&e{amount} &6Coins";

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

    // --------------------------------------------------------------------------------------- by AllFiRE
    // BlockDisplay Settings
    @ConfigEntry(value = "block-display-enabled", required = false)
    public static Boolean BLOCK_DISPLAY_ENABLED = true;

    @ConfigEntry(value = "block-display-type", required = false)
    public static String BLOCK_DISPLAY_TYPE = "ITEM";

    @ConfigEntry(value = "block-display-skull-texture", required = false)
    public static String BLOCK_DISPLAY_SKULL_TEXTURE = "";

    @ConfigEntry(value = "block-display-skull-type", required = false)
    public static String BLOCK_DISPLAY_SKULL_TYPE = "PLAYER_HEAD";

    @ConfigEntry(value = "block-display-item-type", required = false)
    public static String BLOCK_DISPLAY_ITEM_TYPE = "GOLD_INGOT";

    @ConfigEntry(value = "block-display-material", required = false)
    public static String BLOCK_DISPLAY_MATERIAL = "GOLD_BLOCK";

    @ConfigEntry(value = "block-display-scale-x", required = false)
    public static Float BLOCK_DISPLAY_SCALE_X = 0.5f;

    @ConfigEntry(value = "block-display-scale-y", required = false)
    public static Float BLOCK_DISPLAY_SCALE_Y = 0.5f;

    @ConfigEntry(value = "block-display-scale-z", required = false)
    public static Float BLOCK_DISPLAY_SCALE_Z = 0.5f;

    @ConfigEntry(value = "block-display-offset-x", required = false)
    public static Double BLOCK_DISPLAY_OFFSET_X = 0.0;

    @ConfigEntry(value = "block-display-offset-y", required = false)
    public static Double BLOCK_DISPLAY_OFFSET_Y = 0.1;

    @ConfigEntry(value = "block-display-offset-z", required = false)
    public static Double BLOCK_DISPLAY_OFFSET_Z = 0.0;

    @ConfigEntry(value = "block-display-center-on-block", required = false)
    public static Boolean BLOCK_DISPLAY_CENTER_ON_BLOCK = true;

    @ConfigEntry(value = "block-display-translation-x", required = false)
    public static Float BLOCK_DISPLAY_TRANSLATION_X = 0.0f;

    @ConfigEntry(value = "block-display-translation-y", required = false)
    public static Float BLOCK_DISPLAY_TRANSLATION_Y = 0.0f;

    @ConfigEntry(value = "block-display-translation-z", required = false)
    public static Float BLOCK_DISPLAY_TRANSLATION_Z = 0.0f;

    @ConfigEntry(value = "block-display-rotation-enabled", required = false)
    public static Boolean BLOCK_DISPLAY_ROTATION_ENABLED = true;

    @ConfigEntry(value = "block-display-rotation-speed", required = false)
    public static Float BLOCK_DISPLAY_ROTATION_SPEED = 2.0f;

    @ConfigEntry(value = "block-display-rotation-axis", required = false)
    public static String BLOCK_DISPLAY_ROTATION_AXIS = "Y";

    @ConfigEntry(value = "block-display-billboard", required = false)
    public static String BLOCK_DISPLAY_BILLBOARD = "CENTER";

    @ConfigEntry(value = "block-display-duration", required = false)
    public static Integer BLOCK_DISPLAY_DURATION = 0;

    @ConfigEntry(value = "block-display-pickup-duration", required = false)
    public static Integer BLOCK_DISPLAY_PICKUP_DURATION = 3;

    @ConfigEntry(value = "block-display-animation-enabled", required = false)
    public static Boolean BLOCK_DISPLAY_ANIMATION_ENABLED = true;

    @ConfigEntry(value = "block-display-animation-duration", required = false)
    public static Integer BLOCK_DISPLAY_ANIMATION_DURATION = 40;

    @ConfigEntry(value = "block-display-animation-bobbing", required = false)
    public static Boolean BLOCK_DISPLAY_ANIMATION_BOBBING = true;

    @ConfigEntry(value = "block-display-animation-bob-height", required = false)
    public static Double BLOCK_DISPLAY_ANIMATION_BOB_HEIGHT = 0.1;

    @ConfigEntry(value = "block-display-animation-pulse", required = false)
    public static Boolean BLOCK_DISPLAY_ANIMATION_PULSE = false;

    @ConfigEntry(value = "block-display-animation-pulse-amount", required = false)
    public static Float BLOCK_DISPLAY_ANIMATION_PULSE_AMOUNT = 0.1f;

    @ConfigEntry(value = "block-display-brightness-enabled", required = false)
    public static Boolean BLOCK_DISPLAY_BRIGHTNESS_ENABLED = true;

    @ConfigEntry(value = "block-display-brightness-block", required = false)
    public static Integer BLOCK_DISPLAY_BRIGHTNESS_BLOCK = 15;

    @ConfigEntry(value = "block-display-brightness-sky", required = false)
    public static Integer BLOCK_DISPLAY_BRIGHTNESS_SKY = 15;

    @ConfigEntry(value = "block-display-glow-enabled", required = false)
    public static Boolean BLOCK_DISPLAY_GLOW_ENABLED = false;

    @ConfigEntry(value = "block-display-glow-color", required = false)
    public static String BLOCK_DISPLAY_GLOW_COLOR = "FFD700";

    @ConfigEntry(value = "block-display-view-range", required = false)
    public static Float BLOCK_DISPLAY_VIEW_RANGE = 32.0f;

    @ConfigEntry(value = "coin-display-type", required = false)
    public static String COIN_DISPLAY_TYPE = "item";

    @ConfigEntry(value = "click-pickup-enabled", required = false)
    public static Boolean CLICK_PICKUP_ENABLED = false;

    @ConfigEntry(value = "click-pickup-button", required = false)
    public static String CLICK_PICKUP_BUTTON = "RIGHT";
}
