package me.justeli.coins.language;

import net.kyori.adventure.text.Component;

/**
 * a language entry that gets parsed to mini-message and can be filled with replacements
 * @author Eli
 * @since April 24, 2026
 */
public final class FillEntry extends FormatEntry {
    private FillEntry(String message) {
        super(message);
    }

    public static FillEntry of(String message) {
        return new FillEntry(message);
    }

    public Component with(EntryReplacement.Filled... replacements) {
        Component target = component;
        for (EntryReplacement.Filled replacement : replacements) {
            target = target.replaceText(builder ->
                builder.matchLiteral("{" + replacement.getIdentifier() + "}").replacement(replacement.getReplacement())
            );
        }
        return target;
    }
}
