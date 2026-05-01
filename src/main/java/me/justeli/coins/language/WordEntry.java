package me.justeli.coins.language;

import me.justeli.coins.util.Util;
import net.kyori.adventure.text.Component;

/**
 * @author Eli
 * @since April 24, 2026
 */
public final class WordEntry extends Entry {
    private WordEntry(String word) {
        super(word);
    }

    public static WordEntry of(String message) {
        return new WordEntry(message);
    }

    @Override
    public Component getComponent() {
        return Component.text(raw);
    }

    public Component getCapitalizedComponent() {
        return Component.text(Util.toCapitalized(raw));
    }

    public String getCapitalized() {
        return Util.toCapitalized(raw);
    }
}
