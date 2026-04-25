package me.justeli.coins.language;

import me.justeli.coins.util.ColorResolver;
import me.justeli.coins.util.ComponentUtil;
import net.kyori.adventure.text.Component;

/**
 * a language entry that gets parsed to mini-message
 * @author Eli
 * @since April 24, 2026
 */
public class FormatEntry extends Entry {
    protected final Component component;
    protected FormatEntry(String message) {
        super(message);
        this.component = ComponentUtil.parse(message).colorIfAbsent(ColorResolver.PRIMARY);
    }

    public static FormatEntry of(String message) {
        return new FormatEntry(message);
    }

    public Component getComponent() {
        return component;
    }
}
