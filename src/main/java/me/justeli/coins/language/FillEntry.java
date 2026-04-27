package me.justeli.coins.language;

import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

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

    public Component with(EntryReplacement.Filled... fillings) {
        Map<String, Component> replacements = new HashMap<>();
        for (EntryReplacement.Filled replacement : fillings) {
            replacements.put(replacement.getIdentifier(), replacement.getReplacement());
        }

        return component.replaceText(builder ->
            builder.match("\\{([^}]+)}").replacement((match, original) ->
                replacements.getOrDefault(match.group(1), original.build())
            )
        );
    }
}
