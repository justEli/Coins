package me.justeli.coins.command;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.config.Message;
import me.justeli.coins.item.CoinMeta;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import me.justeli.coins.util.PluginVersion;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.TreeSet;

/**
 * @author Eli
 * @since December 26, 2018 (creation)
 */
public final class CoinsCommand implements CommandExecutor, TabCompleter {
    private final Coins coins;
    public CoinsCommand(Coins coins) {
        this.coins = coins;

        var command = coins.getCommand("coins");
        if (command == null) {
            return;
        }

        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    private static final SplittableRandom RANDOM = new SplittableRandom();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            handleSendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (checkPermission(sender, Permissions.hasCommandReload(sender))) {
                    long ms = System.currentTimeMillis();

                    coins.getSettings().reload();
                    coins.getBaseCoin().reload();

                    sender.sendMessage(Message.RELOAD_SUCCESS.replace(Long.toString(System.currentTimeMillis() - ms)));
                    if (coins.getSettings().getWarningCount() != 0) {
                        sender.sendMessage(Message.MINOR_ISSUES.toString());
                    }
                    else {
                        sender.sendMessage(Message.CHECK_SETTINGS.toString());
                    }
                }
            }
            case "settings" -> {
                if (checkPermission(sender, Permissions.hasCommandSettings(sender))) {
                    int page = args.length > 1? Util.parseInt(args[1]).orElse(1) : 1;
                    TreeSet<String> keys = coins.getSettings().getKeys();
                    int totalPages = keys.size() / 8 + Math.min(keys.size() % 8, 1);

                    sender.sendMessage(String.format(COINS_TITLE, "Settings") + Util.color(" &7" + page + "&8/&7" + totalPages));
                    for (String setting : Util.page(new ArrayList<>(keys), 8, page)) {
                        sender.sendMessage(Util.color(setting));
                    }
                }
            }
            case "drop" -> {
                if (checkPermission(sender, Permissions.hasCommandDrop(sender))) {
                    handleDropCoins(sender, args);
                }
            }
            case "remove" -> {
                if (checkPermission(sender, Permissions.hasCommandRemove(sender))) {
                    handleRemoveCoins(sender, args);
                }
            }
            case "lang", "language" -> {
                if (checkPermission(sender, Permissions.hasCommandLanguage(sender))) {
                    for (Message message : Message.values()) {
                        sender.sendMessage(message.toString());
                    }
                }
            }
            case "version", "update" -> {
                if (checkPermission(sender, Permissions.hasCommandVersion(sender))) {
                    sender.sendMessage(String.format(COINS_TITLE, "Version"));

                    Optional<PluginVersion> latestVersion = coins.getLatestVersion();
                    String currentVersion = coins.getDescription().getVersion();

                    sender.sendMessage(Message.CURRENTLY_INSTALLED.replace(currentVersion));

                    if (latestVersion.isEmpty()) {
                        sender.sendMessage(Message.LATEST_RETRIEVE_FAIL.toString());
                    }
                    else if (latestVersion.get().tag().equals(currentVersion)) {
                        sender.sendMessage(Message.UP_TO_DATE.replace(currentVersion));
                    }
                    else {
                        sender.sendMessage(Message.LATEST_RELEASE.replace(
                            latestVersion.get().tag(),
                            Util.DATE_FORMAT.format(new Date(latestVersion.get().time())),
                            latestVersion.get().name(),
                            coins.getDescription().getWebsite()
                        ));
                    }
                }
            }
            case "toggle" -> {
                if (checkPermission(sender, Permissions.hasCommandToggle(sender))) {
                    Message message = coins.toggleDisabled()? Message.ENABLED : Message.DISABLED;
                    sender.sendMessage(Message.GLOBALLY_DISABLED_INFORM.replace(message.toString()));
                    if (coins.isDisabled()) {
                        sender.sendMessage(Message.DISABLED_DESCRIPTION.toString());
                    }
                }
            }
            default -> handleSendHelp(sender);
        }

        return true;
    }

    private boolean checkPermission(CommandSender sender, boolean permission) {
        if (permission) {
            return true;
        }

        sender.sendMessage(Message.NO_PERMISSION.toString());
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String remaining = args[0].toLowerCase();
            if (Permissions.hasCommandDrop(sender) && "drop".startsWith(remaining)) {
                list.add("drop");
            }
            if (Permissions.hasCommandReload(sender) && "reload".startsWith(remaining)) {
                list.add("reload");
            }
            if (Permissions.hasCommandSettings(sender) && "settings".startsWith(remaining)) {
                list.add("settings");
            }
            if (Permissions.hasCommandVersion(sender) && "version".startsWith(remaining)) {
                list.add("version");
            }
            if (Permissions.hasCommandRemove(sender) && "remove".startsWith(remaining)) {
                list.add("remove");
            }
            if (Permissions.hasCommandToggle(sender) && "toggle".startsWith(remaining)) {
                list.add("toggle");
            }
        }
        else if (args.length == 2) {
            String remaining = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("remove") && Permissions.hasCommandRemove(sender)) {
                if ("all".startsWith(remaining)) {
                    list.add("all");
                }
                list.add("[radius]");
            }
            if (args[0].equalsIgnoreCase("drop") && Permissions.hasCommandDrop(sender)) {
                for (Player onlinePlayer : coins.getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(remaining)) {
                        list.add(onlinePlayer.getName());
                    }
                }
                if (remaining.isEmpty() || remaining.contains(",") || Util.parseInt(remaining).isPresent()) {
                    list.add("<x,y,z>");
                    list.add("<x,y,z,world>");
                }
            }
            if (args[0].equalsIgnoreCase("settings") && Permissions.hasCommandSettings(sender)) {
                for (int i = 1; i < 8; i++) {
                    list.add(Integer.toString(i));
                }
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("remove") && Permissions.hasCommandRemove(sender)) {
                list.add("<amount>");
            }
            else if (args[0].equalsIgnoreCase("drop") && Permissions.hasCommandDrop(sender)) {
                list.add("<amount>");
            }
        }
        else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("remove") && Permissions.hasCommandRemove(sender)) {
                list.add("[radius]");
            }
            else if (args[0].equalsIgnoreCase("drop") && Permissions.hasCommandDrop(sender)) {
                list.add("[radius]");
            }
        }

        return list;
    }

    private void handleDropCoins(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Message.DROP_USAGE.toString());
            return;
        }

        Optional<Player> onlinePlayer = Util.getOnlinePlayer(args[1]);
        Optional<Integer> amount = Util.parseInt(args[2]);
        if (amount.isEmpty()) {
            sender.sendMessage(Message.INVALID_NUMBER.toString());
            return;
        }

        int radius = amount.get() / 20;
        if (radius < 2) {
            radius = 2;
        }

        if (args.length >= 4) {
            Optional<Integer> inputRadius = Util.parseInt(args[3]);
            if (inputRadius.isEmpty()) {
                sender.sendMessage(Message.INVALID_NUMBER.toString());
                return;
            }

            radius = inputRadius.get();
        }

        Location location;
        String name;
        if (onlinePlayer.isEmpty()) {
            if (!args[1].contains(",")) {
                sender.sendMessage(Message.PLAYER_NOT_FOUND.toString());
                return;
            }
            else {
                String[] coords = args[1].split(",");

                Optional<Double> x = Util.parseDouble(coords[0]);
                Optional<Double> y = Util.parseDouble(coords[1]);
                Optional<Double> z = Util.parseDouble(coords[2]);

                if (x.isPresent() && y.isPresent() && z.isPresent()) {
                    World world = null;
                    if (coords.length == 4) {
                        world = coins.getServer().getWorld(coords[3]);
                    }
                    if (world == null && sender instanceof Player player) {
                        world = player.getWorld();
                    }
                    if (world == null) {
                        world = coins.getServer().getWorlds().get(0);
                    }

                    location = new Location(world, x.get(), y.get(), z.get());
                    name = Util.doubleToString(x.get()) + ", " + Util.doubleToString(y.get()) + ", " + Util.doubleToString(z.get());
                }
                else {
                    sender.sendMessage(Message.COORDS_NOT_FOUND.toString());
                    return;
                }
            }
        }
        else {
            location = onlinePlayer.get().getLocation();
            name = onlinePlayer.get().getName();
        }

        if (Util.isDisabledHere(location.getWorld())) {
            sender.sendMessage(Message.COINS_DISABLED.toString());
            return;
        }

        if (radius < 1 || radius > 80) {
            sender.sendMessage(Message.INVALID_RADIUS.toString());
            return;
        }

        if (amount.get() < 1 || amount.get() > 1000) {
            sender.sendMessage(Message.INVALID_AMOUNT.toString());
            return;
        }

        dropCoins(location, radius, amount.get());
        sender.sendMessage(Message.SPAWNED_COINS.replace(
            Long.toString(amount.get()),
            Long.toString(radius),
            name
        ));
    }

    private void handleRemoveCoins(CommandSender sender, String[] args) {
        Collection<Item> items = coins.getServer().getWorlds().get(0).getEntitiesByClass(Item.class);
        double radius = 0;

        if (args.length >= 2 && sender instanceof Player) {
            if (!args[1].equalsIgnoreCase("all")) {
                Optional<Integer> inputRadius = Util.parseInt(args[1]);
                if (inputRadius.isEmpty()) {
                    sender.sendMessage(Message.INVALID_RADIUS.toString());
                    return;
                }

                radius = inputRadius.get();
                if (radius < 1 || radius > 80) {
                    sender.sendMessage(Message.INVALID_RADIUS.toString());
                    return;
                }
            }
        }

        if (sender instanceof Player player) {
            if (radius == 0) {
                items = player.getWorld().getEntitiesByClass(Item.class);
            }
            else {
                Collection<Item> nearbyItems = new ArrayList<>();
                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof Item item) {
                        nearbyItems.add(item);
                    }
                }
                items = nearbyItems;
            }
        }

        long amount = 0;
        for (Item item : items) {
            if (!coins.getCoinMeta().isCoin(item.getItemStack())) {
                continue;
            }

            float random = RANDOM.nextFloat() * 3F;
            item.setVelocity(new Vector(0, random, 0));

            // works on Folia
            coins.getScheduler().runEntityTaskLater(item, (long) (random * 5F), item::remove);

            amount++;
        }

        sender.sendMessage(Message.REMOVED_COINS.replace(Long.toString(amount)));
    }

    private static final String COINS_TITLE = Util.color("&8&m     &6 Coins &e%s &8&m     &r");

    private void handleSendHelp(CommandSender sender) {
        String currentVersion = coins.getDescription().getVersion();
        Optional<PluginVersion> latestVersion = coins.getLatestVersion();

        int lines = 0;

        String notice = "";
        if (coins.isDisabled()) {
            notice = " " + Message.GLOBALLY_DISABLED;
        }
        else if (latestVersion.isPresent() && !latestVersion.get().tag().equals(currentVersion) && Permissions.hasCommandVersion(sender)) {
            notice = " " + Message.OUTDATED.replace("/coins update");
        }

        sender.sendMessage(String.format(COINS_TITLE, currentVersion) + ChatColor.DARK_RED + notice);

        if (Permissions.hasCommandDrop(sender)) {
            sender.sendMessage(Message.DROP_USAGE.toString());
            lines++;
        }
        if (Permissions.hasCommandRemove(sender)) {
            sender.sendMessage(Message.REMOVE_USAGE.toString());
            lines++;
        }
        if (Permissions.hasCommandSettings(sender)) {
            sender.sendMessage(Message.SETTINGS_USAGE.toString());
            lines++;
        }
        if (Permissions.hasCommandReload(sender)) {
            sender.sendMessage(Message.RELOAD_USAGE.toString());
            lines++;
        }
        if (Permissions.hasCommandVersion(sender)) {
            sender.sendMessage(Message.VERSION_CHECK.toString());
            lines++;
        }
        if (Permissions.hasCommandToggle(sender)) {
            sender.sendMessage(Message.TOGGLE_USAGE.toString());
            lines++;
        }
        if (Config.ENABLE_WITHDRAW && Permissions.hasWithdraw(sender)) {
            sender.sendMessage(Message.WITHDRAW_USAGE.toString());
            lines++;
        }

        if (lines == 0) {
            sender.sendMessage(ChatColor.GOLD + coins.getDescription().getDescription());
            sender.sendMessage(ChatColor.YELLOW + "More info: " + ChatColor.BLUE + coins.getDescription().getWebsite());
        }
    }

    private void dropCoins(Location location, int radius, int amount) {
        if (location.getWorld() == null) {
            return;
        }

        Location dropLocation = location.add(0.0, 0.5, 0.0);
        ItemStack coin = coins.getCreateCoin().createDropped();

        coins.getScheduler().runLocationTaskRepeated(location, amount, 1, () -> {
            Item item = location.getWorld().dropItem(
                dropLocation,
                coins.meta(coin).setData(CoinMeta.COINS_RANDOM, RANDOM.nextDouble()).build()
            );

            item.setPickupDelay(30);
            item.setVelocity(new Vector(
                (RANDOM.nextDouble() - 0.5) * radius / 10,
                RANDOM.nextDouble() * radius / 5,
                (RANDOM.nextDouble() - 0.5) * radius / 10
            ));
        });
    }
}
