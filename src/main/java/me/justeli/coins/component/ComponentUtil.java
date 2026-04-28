package me.justeli.coins.component;

import me.justeli.coins.config.Config;
import me.justeli.coins.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * @author Eli
 * @since April 20, 2026
 */
public final class ComponentUtil {
    // components
    public static final Component BRAND_COMPONENT = Component.text("Coins", ColorResolver.COINS);

    // utils
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .strict(false)
        .editTags(builder -> builder.resolver(new ColorResolver()))
        .build();

    /// parse a String of MiniMessage format to Component
    public static Component parse(String message) {
        return message.isEmpty()? Component.empty() : MINI_MESSAGE.deserialize(message);
    }

    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    public static String toStripped(Component component) {
        return PLAIN_TEXT_SERIALIZER.serialize(component);
    }

    /// replaces {amount} AND {currency} to appropriate values
    public static Component replaceAmount(Component component, double amount) {
        return replaceCurrency(component).replaceText(builder ->
            builder.matchLiteral("{amount}").replacement(Util.toFormattedMoneyDecimals(amount))
        );
    }

    /// replaces {currency} to Config#CURRENCY_SYMBOL
    public static Component replaceCurrency(Component component) {
        return component.replaceText(builder ->
            builder.matchLiteral("{currency}").replacement(Config.CURRENCY_SYMBOL)
        );
    }

    // legacy stuff

    private static final LegacyComponentSerializer SERIALIZER =
        LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    public static String toSpigotDisplayName(Component component) {
        return SERIALIZER.serialize(component);
    }

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[§&]#?[xXa-fA-Fi-oI-O0-9]");
    public static boolean isLegacyColored(String message) {
        return LEGACY_COLOR_PATTERN.matcher(message).find();
    }

    // 1. convert '&.' to '<.>'
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacy('&');
    private static String convertNormalCodes(@NotNull String message) {
        return MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(message));
    }

    // 2. convert '§x§.§.§.§.§.§.' to '<#......>'
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("§[xX]((?:§[a-fA-F0-9]){6})");
    private static String convertSpigotHexCodes(@NotNull String message) {
        return SPIGOT_HEX_PATTERN.matcher(message).replaceAll(match -> "<#" + match.group(1).replace("§", "") + ">");
    }

    // 3. convert '&#......' to '<#......>'
    private static final Pattern HEX_PATTERN = Pattern.compile("(?<!\\\\)&#([a-fA-F0-9]{6})");
    private static String convertHexCodes(@NotNull String message) {
        return HEX_PATTERN.matcher(message).replaceAll(match -> "<#" + match.group(1) + ">");
    }

    public static String parseLegacyToMiniMessage(@NotNull String message) {
        return convertHexCodes(convertSpigotHexCodes(convertNormalCodes(message)));
    }
}
