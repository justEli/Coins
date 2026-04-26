package me.justeli.coins.command;

import me.justeli.coins.Coins;
import me.justeli.coins.language.Language;
import me.justeli.coins.util.ColorResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * @author Eli
 * @since August 2, 2021 (creation)
 */
public abstract class DisabledCommandLogic {
    private final Coins coins;
    public DisabledCommandLogic(Coins coins) {
        this.coins = coins;
    }

    public void executeCommand(@NotNull CommandSender sender) {
        coins.getMessenger().sendMessage(sender, Language.COMMAND_DISABLED);
        for (String message : coins.getProblems()) {
            coins.getMessenger().sendMessage(sender, Component.text("- " + message, ColorResolver.ERROR));
        }
    }
}
