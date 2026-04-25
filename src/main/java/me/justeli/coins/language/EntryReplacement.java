package me.justeli.coins.language;

import me.justeli.coins.util.ColorResolver;
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
            this.replacement = Component.text(replacement.toString(), color);
        }

        public String getIdentifier() {
            return identifier;
        }

        public Component getReplacement() {
            return replacement;
        }
    }
}
