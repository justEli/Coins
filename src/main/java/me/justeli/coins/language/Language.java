package me.justeli.coins.language;

/**
 * @author Eli
 * @since April 24, 2017 (creation); April 22, 2026 (rewrite)
 */
public final class Language {
    @LanguageEntry("command.coins.drop.dropping")
    public static FillEntry DROP_DROPPING = FillEntry.of("Dropping {amount} coins in a radius of {radius} around {target}.");

    @LanguageEntry("command.coins.drop.usage")
    public static FormatEntry DROP_USAGE = FormatEntry.of("Drop coins around a player or location. An amount, radius, and the value of each coin can be specified.");

    @LanguageEntry("command.coins.reload.perform")
    public static FillEntry RELOAD_PERFORM = FillEntry.of("Config of <coins>Coins</coins> has been reloaded in {duration}ms.");

    @LanguageEntry("command.coins.reload.settings")
    public static FormatEntry RELOAD_SETTINGS = FormatEntry.of("Use <var>/coins settings</var> to see all active settings.");

    @LanguageEntry("command.coins.reload.warnings")
    public static FillEntry RELOAD_WARNINGS = FillEntry.of("<error>Reload complete with {amount} warnings. See console for details.");

    @LanguageEntry("command.coins.remove.removed")
    public static FillEntry REMOVE_REMOVED = FillEntry.of("Removed a total of {amount} coins in the world.");

    @LanguageEntry("command.coins.toggle.disabled")
    public static FormatEntry TOGGLE_DISABLED = FormatEntry.of("When disabled, coins won't drop and withdrawing isn't possible. Picking up existing coins and depositing them will still work.");

    @LanguageEntry("command.coins.toggle.toggled")
    public static FillEntry TOGGLE_TOGGLED = FillEntry.of("Coins has been globally {type}. Toggle with <var>/coins toggle</var>.");

    @LanguageEntry("command.coins.version.current")
    public static FillEntry VERSION_CURRENT = FillEntry.of("Version currently installed: {version}");

    @LanguageEntry("command.coins.version.latest")
    public static FormatEntry VERSION_LATEST = FormatEntry.of("<green>This is the latest version available.");

    @LanguageEntry("command.coins.version.new_release")
    public static FillEntry VERSION_RELEASE = FillEntry.of("Latest version of <coins>Coins</coins> is {version}, released on {date}, and described as: '{description}'. Consider updating the plugin to this version.");

    @LanguageEntry("command.coins.version.retrieve_fail")
    public static FormatEntry VERSION_FAIL = FormatEntry.of("<error>Couldn't retrieve the latest version of <coins>Coins</coins>.");

    @LanguageEntry("command.disabled_reasons")
    public static FormatEntry COMMAND_DISABLED = FormatEntry.of("Plugin <coins>Coins</coins> is disabled for the following reason(s):");

    @LanguageEntry("command.invalid_location")
    public static FormatEntry COMMAND_INVALID_LOCATION = FormatEntry.of("<error>Cannot find a location from given input.");

    @LanguageEntry("command.invalid_number")
    public static FillEntry COMMAND_INVALID_NUMBER = FillEntry.of("<error>Given input for '{type}' is an invalid number.");

    @LanguageEntry("command.invalid_player")
    public static FormatEntry COMMAND_INVALID_PLAYER = FormatEntry.of("<error>Cannot find an online player from given input.");

    @LanguageEntry("command.invalid_range")
    public static FillEntry COMMAND_INVALID_RANGE = FillEntry.of("<error>Given input for '{type}' must be between {min} and {max}.");

    @LanguageEntry("command.no_permission")
    public static FormatEntry COMMAND_NO_PERMISSION = FormatEntry.of("<error>You do not have access to that command.");

    @LanguageEntry("command.withdraw.disabled")
    public static FormatEntry WITHDRAW_DISABLED = FormatEntry.of("<error>Withdrawing coins is disabled on this server.");

    @LanguageEntry("command.withdraw.full_inventory")
    public static FormatEntry WITHDRAW_FULL_INVENTORY = FormatEntry.of("<error>Unable to withdraw coins because your inventory is full.");

    @LanguageEntry("command.withdraw.usage")
    public static FormatEntry WITHDRAW_USAGE = FormatEntry.of("Withdraw money from your balance into physical coins.");

    @LanguageEntry("command.withdraw.withdrew")
    public static FillEntry WITHDRAW_WITHDREW = FillEntry.of("Withdrew {currency}{amount} from balance as a physical coin.");

    @LanguageEntry("general.disabled")
    public static FormatEntry GENERAL_DISABLED = FormatEntry.of("<error>Coins are disabled in this world.");

    @LanguageEntry("page.not_found")
    public static FormatEntry PAGE_NOT_FOUND = FormatEntry.of("<error>No content found for given page.");

    @LanguageEntry("word.amount")
    public static WordEntry WORD_AMOUNT = WordEntry.of("amount");

    @LanguageEntry("word.disabled")
    public static WordEntry WORD_DISABLED = WordEntry.of("disabled");

    @LanguageEntry("word.enabled")
    public static WordEntry WORD_ENABLED = WordEntry.of("enabled");

    @LanguageEntry("word.language")
    public static WordEntry WORD_LANGUAGE = WordEntry.of("language");

    @LanguageEntry("word.outdated")
    public static WordEntry WORD_OUTDATED = WordEntry.of("outdated");

    @LanguageEntry("word.page")
    public static WordEntry WORD_PAGE = WordEntry.of("page");

    @LanguageEntry("word.player")
    public static WordEntry WORD_PLAYER = WordEntry.of("player");

    @LanguageEntry("word.radius")
    public static WordEntry WORD_RADIUS = WordEntry.of("radius");

    @LanguageEntry("word.settings")
    public static WordEntry WORD_SETTINGS = WordEntry.of("settings");

    @LanguageEntry("word.value")
    public static WordEntry WORD_VALUE = WordEntry.of("value");

    @LanguageEntry("word.version")
    public static WordEntry WORD_VERSION = WordEntry.of("version");

    @LanguageEntry("word.world")
    public static WordEntry WORD_WORLD = WordEntry.of("world");
}
