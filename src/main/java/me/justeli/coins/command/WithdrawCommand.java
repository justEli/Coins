package me.justeli.coins.command;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.config.Message;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eli
 * @since December 26, 2018 (creation)
 */
public final class WithdrawCommand implements CommandExecutor, TabCompleter {
    private final Coins coins;
    public WithdrawCommand(Coins coins) {
        this.coins = coins;

        var command = coins.getCommand("withdraw");
        if (command == null) {
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (coins.isDisabled()) {
            sender.sendMessage(Message.COINS_DISABLED.toString());
            return true;
        }

        if (!Config.ENABLE_WITHDRAW) {
            sender.sendMessage(Message.WITHDRAWING_DISABLED.toString());
            return true;
        }

        if (!Permissions.hasWithdraw(sender) || !(sender instanceof Player player)) {
            Util.sendNoPermission(sender);
            return true;
        }

        if (Util.isDisabledHere(player.getWorld())) {
            sender.sendMessage(Message.COINS_DISABLED.toString());
            return true;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Message.INVENTORY_FULL.toString());
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Message.WITHDRAW_USAGE.toString());
            return true;
        }

        double worth = Util.round(Util.parseDouble(args[0]).orElse(0D));
        int amount = args.length >= 2? Util.parseInt(args[1]).orElse(0) : 1;
        double total = worth * amount;

        if (worth <= 0 || amount < 1 || total <= 0 || amount > 64) {
            sender.sendMessage(Message.INVALID_AMOUNT.toString());
            return true;
        }

        if (worth <= Config.MAX_WITHDRAW_AMOUNT) {
            coins.getEconomy().canAfford(player.getUniqueId(), total, canAfford -> {
                if (canAfford) {
                    coins.getEconomy().withdraw(player.getUniqueId(), total, () -> {
                        ItemStack coin = coins.getCreateCoin().createWithdrawn(worth);
                        coin.setAmount(amount);

                        player.getInventory().addItem(coin);
                        player.sendMessage(Message.WITHDRAW_COINS.replace(Util.doubleToString(total)));

                        if (!Config.WITHDRAW_MESSAGE.isEmpty()) {
                            Util.sendMessage(player, Config.WITHDRAW_MESSAGE, Config.WITHDRAW_MESSAGE_POSITION, total);
                        }
                    });
                }
                else {
                    player.sendMessage(Message.NOT_THAT_MUCH.toString());
                }
            });
        }
        else {
            player.sendMessage(Message.NOT_THAT_MUCH.toString());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> numbers = new ArrayList<>();
        if (args.length == 1) {
            numbers.add("<worth (1 to " + Config.MAX_WITHDRAW_AMOUNT.intValue() + ")>");
        }
        else if (args.length == 2) {
            numbers.add("[amount]");
        }
        return numbers;
    }
}
