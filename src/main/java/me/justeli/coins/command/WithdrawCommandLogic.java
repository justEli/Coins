package me.justeli.coins.command;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.language.EntryReplacement;
import me.justeli.coins.language.Language;
import me.justeli.coins.component.ColorResolver;
import me.justeli.coins.component.ComponentUtil;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Eli
 * @since December 26, 2018 (creation)
 */
public abstract class WithdrawCommandLogic {
    private final Coins coins;
    public WithdrawCommandLogic(Coins coins) {
        this.coins = coins;
    }

    private static final EntryReplacement FILL_TYPE = new EntryReplacement("type");
    private static final EntryReplacement FILL_MIN = new EntryReplacement("min");
    private static final EntryReplacement FILL_MAX = new EntryReplacement("max");

    private static final EntryReplacement FILL_CURRENCY = new EntryReplacement("currency", ColorResolver.MONEY);
    private static final EntryReplacement FILL_AMOUNT = new EntryReplacement("amount", ColorResolver.MONEY);

    public void executeCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!Permissions.hasWithdraw(sender) || !(sender instanceof Player player)) {
            coins.getMessenger().sendMessage(sender, Language.COMMAND_NO_PERMISSION);
            return;
        }

        if (coins.isDisabled() || Util.isDisabledHere(player.getWorld())) {
            coins.getMessenger().sendMessage(sender, Language.GENERAL_DISABLED);
            return;
        }

        if (!Config.ENABLE_WITHDRAW) {
            coins.getMessenger().sendMessage(sender, Language.WITHDRAW_DISABLED);
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            coins.getMessenger().sendMessage(sender, Language.WITHDRAW_FULL_INVENTORY);
            return;
        }

        if (args.length == 0) {
            coins.getMessenger().sendMessage(sender, Component.text("/withdraw <%s> [%s]".formatted(
                Language.WORD_VALUE, Language.WORD_AMOUNT
            ), ColorResolver.VAR));
            coins.getMessenger().sendMessage(sender, Language.WITHDRAW_USAGE);
            return;
        }

        double worth = Util.toRoundedMoneyDecimals(Util.parseDouble(args[0]).orElse(0D));
        if (worth <= 0 || worth > Config.MAX_WITHDRAW_AMOUNT) {
            coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_RANGE.with(
                FILL_TYPE.filled(Language.WORD_VALUE), FILL_MIN.filled(0), FILL_MAX.filled(Config.MAX_WITHDRAW_AMOUNT)
            ));
            return;
        }

        int amount = args.length >= 2? Util.parseInt(args[1]).orElse(0) : 1;
        double total = worth * amount;
        if (amount < 1 || total <= 0 || amount > 64) {
            coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_RANGE.with(
                FILL_TYPE.filled(Language.WORD_AMOUNT), FILL_MIN.filled(1), FILL_MAX.filled(64)
            ));
            return;
        }

        coins.getEconomy().balance(player.getUniqueId(), balance -> {
            coins.getEconomy().canAfford(player.getUniqueId(), total, canAfford -> {
                if (!canAfford) {
                    coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_RANGE.with(
                        FILL_TYPE.filled(Language.WORD_VALUE), FILL_MIN.filled(0), FILL_MAX.filled(balance)
                    ));
                    return;
                }

                coins.getEconomy().withdraw(player.getUniqueId(), total, () -> {
                    ItemStack coin = coins.getCreateCoin().createWithdrawn(worth);
                    coin.setAmount(amount);

                    player.getInventory().addItem(coin);
                    coins.getMessenger().sendMessage(sender, Language.WITHDRAW_WITHDREW.with(
                        FILL_CURRENCY.filled(Config.CURRENCY_SYMBOL),
                        FILL_AMOUNT.filled(Util.toFormattedMoneyDecimals(total))
                    ));

                    if (!Config.WITHDRAW_MESSAGE.equals(Component.empty())) {
                        coins.getMessenger().sendMessage(
                            player, Config.WITHDRAW_MESSAGE_POSITION,
                            ComponentUtil.replaceAmount(Config.WITHDRAW_MESSAGE, total)
                        );
                    }
                });
            });
        });
    }

    public List<String> getTabCompletions(@NotNull String[] args) {
        if (args.length <= 1) {
            return List.of("[%s]".formatted(Language.WORD_VALUE));
        }
        else if (args.length == 2) {
            return List.of("[%s]".formatted(Language.WORD_AMOUNT));
        }
        return List.of();
    }
}
