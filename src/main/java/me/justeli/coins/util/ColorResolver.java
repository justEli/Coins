package me.justeli.coins.util;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eli
 * @since April 23, 2026
 */
public final class ColorResolver implements TagResolver {
    private static final Map<String, TextColor> KEYS = new HashMap<>();

    // https://coolors.co/bf1111-ff0000-ff7711-ffc430-ffcc88-6dd47e-148c30-95a5a6
    public static final TextColor DOWN = TextColor.color(0xBF1111); // todo should be configurable imo
    public static final TextColor ERROR = TextColor.color(0xFF0000);
    public static final TextColor PRIMARY = TextColor.color(0xFF7711);
    public static final TextColor COINS = TextColor.color(0xFFC430);
    public static final TextColor VAR = TextColor.color(0xFFCC88);
    public static final TextColor MONEY = TextColor.color(0x6DD47E);
    public static final TextColor UP = TextColor.color(0x148C30);
    public static final TextColor USER = TextColor.color(0x95A5A6);

    static {
        // related to Coins
        KEYS.put("coins", COINS);
        KEYS.put("primary", PRIMARY);
        KEYS.put("error", ERROR);
        KEYS.put("var", VAR);
        KEYS.put("money", MONEY);
        KEYS.put("user", USER);
        KEYS.put("down", DOWN);
        KEYS.put("up", UP);

        // bedrock edition colors
        KEYS.put("minecoin_gold", TextColor.color(0xDDD605));
        KEYS.put("material_quartz", TextColor.color(0xE3D4D1));
        KEYS.put("material_iron", TextColor.color(0xCECACA));
        KEYS.put("material_netherite", TextColor.color(0x443A3B));
        KEYS.put("material_redstone", TextColor.color(0x971607));
        KEYS.put("material_copper", TextColor.color(0xB4684D));
        KEYS.put("material_gold", TextColor.color(0xDEB12D));
        KEYS.put("material_emerald", TextColor.color(0x47A036));
        KEYS.put("material_diamond", TextColor.color(0x2CBAA8));
        KEYS.put("material_lapis", TextColor.color(0x21497B));
        KEYS.put("material_amethyst", TextColor.color(0x9A5CC6));
        KEYS.put("material_resin", TextColor.color(0xEB7114));
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (KEYS.containsKey(name)) {
            return Tag.styling(KEYS.get(name));
        }

        return null;
    }

    @Override
    public boolean has(@NotNull String name) {
        return KEYS.containsKey(name);
    }
}
