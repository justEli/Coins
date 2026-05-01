package me.justeli.coins.language;

import me.justeli.coins.component.ColorResolver;
import me.justeli.coins.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * @author Eli
 * @since April 24, 2026
 */
public final class EntryReplacement {
    private final String identifier;
    private final TextColor color;

    public EntryReplacement(String identifier) {
        this.identifier = identifier;
        this.color = ColorResolver.VAR;
    }

    public EntryReplacement(String identifier, TextColor color) {
        this.identifier = identifier;
        this.color = color;
    }

    public Filled filled(Object replacement) {
        return new Filled(replacement);
    }

    public class Filled {
        private final Component replacement;
        public Filled(Object replacement) {
            String value = replacement instanceof Double number? Util.toFormattedMoneyDecimals(number) : replacement.toString();
            this.replacement = Component.text(value, color);
        }

        public String getIdentifier() {
            return identifier;
        }

        public Component getReplacement() {
            return replacement;
        }
    }
}
