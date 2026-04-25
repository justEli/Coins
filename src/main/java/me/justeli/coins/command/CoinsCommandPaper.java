package me.justeli.coins.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.justeli.coins.Coins;

import java.util.Collection;
import java.util.List;

/**
 * @author Eli
 * @since April 21, 2026 (creation)
 */
public final class CoinsCommandPaper extends CoinsCommandLogic implements BasicCommand {
    public CoinsCommandPaper(Coins coins) {
        super(coins);
        coins.getLifecycleManager().registerEventHandler(
            LifecycleEvents.COMMANDS,
            commands -> commands.registrar().register(
                "coins",
                "Command for showing all available commands from Coins. Also used for various admin tools.",
                List.of("coin"),
                this
            )
        );
    }

    @Override
    public String permission() {
        return "coins.command";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        executeCommand(source.getSender(), args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        return getTabCompletions(commandSourceStack.getSender(), args);
    }
}
