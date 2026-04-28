package me.justeli.coins.command;

import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.language.EntryReplacement;
import me.justeli.coins.language.Language;
import me.justeli.coins.item.CoinMeta;
import me.justeli.coins.component.ColorResolver;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.VersionPlugin;
import me.justeli.coins.util.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * @author Eli
 * @since December 26, 2018 (creation)
 */
public abstract class CoinsCommandLogic {
    private final Coins coins;
    public CoinsCommandLogic(Coins coins) {
        this.coins = coins;
    }

    private static final SplittableRandom RANDOM = new SplittableRandom();

    private static final EntryReplacement FILL_DURATION = new EntryReplacement("duration");
    private static final EntryReplacement FILL_AMOUNT = new EntryReplacement("amount");
    private static final EntryReplacement FILL_VERSION = new EntryReplacement("version");
    private static final EntryReplacement FILL_DATE = new EntryReplacement("date");
    private static final EntryReplacement FILL_DESCRIPTION = new EntryReplacement("description");
    private static final EntryReplacement FILL_TYPE = new EntryReplacement("type");
    private static final EntryReplacement.Filled FILL_MIN_1 = new EntryReplacement("min").filled(1);
    private static final EntryReplacement FILL_MAX = new EntryReplacement("max");
    private static final EntryReplacement FILL_RADIUS = new EntryReplacement("radius");
    private static final EntryReplacement FILL_TARGET = new EntryReplacement("target");

    public void executeCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            handleSendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (checkPermission(sender, Permissions.hasCommandReload(sender))) {
                    long millis = System.currentTimeMillis();

                    coins.getSettings().reload();
                    coins.getBaseCoin().reload();

                    long duration = System.currentTimeMillis() - millis;
                    coins.getMessenger().sendMessage(sender, Language.RELOAD_PERFORM.with(FILL_DURATION.filled(duration)));

                    var amount = coins.getSettings().getWarningCount();
                    if (amount != 0) {
                        coins.getMessenger().sendMessage(sender, Language.RELOAD_WARNINGS.with(FILL_AMOUNT.filled(amount)));
                    }
                    else {
                        coins.getMessenger().sendMessage(sender, Language.RELOAD_SETTINGS);
                    }
                }
            }
            case "settings" -> {
                if (checkPermission(sender, Permissions.hasCommandSettings(sender))) {
                    int page = args.length > 1? Util.parseInt(args[1]).orElse(1) : 1;
                    List<Component> keys = coins.getSettings().getConfigKeys();
                    coins.getMessenger().sendPage(sender, keys, page, Language.WORD_SETTINGS.getCapitalized(), "/coins settings");
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
                     coins.getMessenger().sendHeader(sender, Language.WORD_LANGUAGE.getCapitalized());
                    // TODO
//                    for (Message message : Message.values()) {
//                        sender.sendMessage(message.toString());
//                    }
                }
            }
            case "version", "update" -> {
                if (checkPermission(sender, Permissions.hasCommandVersion(sender))) {
                    coins.getMessenger().sendHeader(sender, Language.WORD_VERSION.getCapitalized());

                    Optional<VersionPlugin> latestVersion = coins.getVersionCheck().getLatestVersion();
                    String currentVersion = coins.getVersionCheck().getPluginVersion();

                    coins.getMessenger().sendMessage(sender, Language.VERSION_CURRENT.with(FILL_VERSION.filled(currentVersion)));

                    if (latestVersion.isEmpty()) {
                        coins.getMessenger().sendMessage(sender, Language.VERSION_FAIL);
                    }
                    else if (latestVersion.get().getTag().equals(currentVersion)) {
                        coins.getMessenger().sendMessage(sender, Language.VERSION_LATEST);
                    }
                    else {
                        coins.getMessenger().sendMessage(sender, Language.VERSION_RELEASE.with(
                            FILL_VERSION.filled(latestVersion.get().getTag()),
                            FILL_DATE.filled(Util.formatDate(latestVersion.get().getTime())),
                            FILL_DESCRIPTION.filled(latestVersion.get().getName())
                        ));
                        coins.getMessenger().sendMessage(sender, coins.getSettings().getPluginUrl());
                    }
                }
            }
            case "toggle" -> {
                if (checkPermission(sender, Permissions.hasCommandToggle(sender))) {
                    coins.getMessenger().sendMessage(sender, Language.TOGGLE_TOGGLED.with(FILL_TYPE.filled(
                        coins.toggleDisabled()? Language.WORD_ENABLED.toString() : Language.WORD_DISABLED.toString()
                    )));

                    if (coins.isDisabled()) {
                        coins.getMessenger().sendMessage(sender, Language.TOGGLE_DISABLED);
                    }
                }
            }
            default -> handleSendHelp(sender);
        }
    }

    private boolean checkPermission(CommandSender sender, boolean permission) {
        if (permission) {
            return true;
        }

        coins.getMessenger().sendMessage(sender, Language.COMMAND_NO_PERMISSION);
        return false;
    }

    public List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length <= 1) {
            String remaining = args.length == 1? args[0].toLowerCase() : "";
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
                list.add("[%s]".formatted(Language.WORD_RADIUS));
            }
            if (args[0].equalsIgnoreCase("drop") && Permissions.hasCommandDrop(sender)) {
                for (Player onlinePlayer : coins.getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(remaining)) {
                        list.add(onlinePlayer.getName());
                    }
                }
                if (remaining.isEmpty() || remaining.contains(",") || Util.parseInt(remaining).isPresent()) {
                    list.add("<x,y,z>");
                    list.add("<x,y,z,%s>".formatted(Language.WORD_WORLD));
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
                list.add("<%s>".formatted(Language.WORD_AMOUNT));
            }
            else if (args[0].equalsIgnoreCase("drop") && Permissions.hasCommandDrop(sender)) {
                list.add("<%s>".formatted(Language.WORD_AMOUNT));
            }
        }
        else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("remove") && Permissions.hasCommandRemove(sender)) {
                list.add("[%s]".formatted(Language.WORD_RADIUS));
            }
            else if (args[0].equalsIgnoreCase("drop") && Permissions.hasCommandDrop(sender)) {
                list.add("[%s]".formatted(Language.WORD_RADIUS));
            }
        }
        else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("drop") && Permissions.hasCommandDrop(sender)) {
                list.add("[%s]".formatted(Language.WORD_VALUE));
            }
        }

        return list;
    }

    private Component getDropCommand() {
        return Component.text("/coins drop <%s|x,y,z[,%s]> <%s> [%s] [%s]".formatted(
            Language.WORD_PLAYER, Language.WORD_WORLD, Language.WORD_AMOUNT, Language.WORD_RADIUS, Language.WORD_VALUE
        ), ColorResolver.VAR);
    }

    private void handleDropCoins(CommandSender sender, String[] args) {
        if (args.length < 3) {
            coins.getMessenger().sendMessage(sender, getDropCommand());
            coins.getMessenger().sendMessage(sender, Language.DROP_USAGE);
            return;
        }

        Optional<Player> target = Util.getOnlinePlayer(args[1]);
        Optional<Integer> amount = Util.parseInt(args[2]);
        if (amount.isEmpty()) {
            coins.getMessenger().sendMessage(
                sender, Language.COMMAND_INVALID_NUMBER.with(FILL_TYPE.filled(Language.WORD_AMOUNT))
            );
            return;
        }

        int radius = amount.get() / 20;
        if (radius < 2) {
            radius = 2;
        }

        if (args.length >= 4) {
            Optional<Integer> inputRadius = Util.parseInt(args[3]);
            if (inputRadius.isEmpty()) {
                coins.getMessenger().sendMessage(
                    sender, Language.COMMAND_INVALID_NUMBER.with(FILL_TYPE.filled(Language.WORD_RADIUS))
                );
                return;
            }

            radius = inputRadius.get();
        }

        double worth = 0;
        if (args.length >= 5) {
            Optional<Double> inputWorth = Util.parseDouble(args[4]);
            if (inputWorth.isEmpty() || inputWorth.get() < 0) {
                coins.getMessenger().sendMessage(
                    sender, Language.COMMAND_INVALID_NUMBER.with(FILL_TYPE.filled(Language.WORD_VALUE))
                );
                return;
            }

            worth = inputWorth.get();
        }

        if (radius < 1 || radius > 80) {
            coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_RANGE.with(
                FILL_TYPE.filled(Language.WORD_RADIUS), FILL_MIN_1, FILL_MAX.filled(80)
            ));
            return;
        }

        if (amount.get() < 1 || amount.get() > 1000) {
            coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_RANGE.with(
                FILL_TYPE.filled(Language.WORD_AMOUNT), FILL_MIN_1, FILL_MAX.filled(1000)
            ));
            return;
        }

        Location location;
        String name;

        if (target.isEmpty()) {
            if (!args[1].contains(",")) {
                coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_PLAYER);
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
                        world = coins.getServer().getWorlds().getFirst();
                    }

                    location = new Location(world, x.get(), y.get(), z.get());
                    name = "x%.1f, y%.1f, z%.1f".formatted(x.get(), y.get(), z.get());
                }
                else {
                    coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_LOCATION);
                    return;
                }
            }
        }
        else {
            location = target.get().getLocation();
            name = target.get().getName();
        }

        if (Util.isDisabledHere(location.getWorld())) {
            coins.getMessenger().sendMessage(sender, Language.GENERAL_DISABLED);
            return;
        }

        dropCoins(location, radius, amount.get(), worth);
        coins.getMessenger().sendMessage(sender, Language.DROP_DROPPING.with(
            FILL_AMOUNT.filled(amount.get()),
            FILL_RADIUS.filled(radius),
            FILL_TARGET.filled(name)
        ));
    }

    private void handleRemoveCoins(CommandSender sender, String[] args) {
        Collection<Item> items = coins.getServer().getWorlds().getFirst().getEntitiesByClass(Item.class);
        double radius = 0;

        if (args.length >= 2 && sender instanceof Player) {
            if (!args[1].equalsIgnoreCase("all")) {
                Optional<Integer> inputRadius = Util.parseInt(args[1]);
                if (inputRadius.isEmpty()) {
                    coins.getMessenger().sendMessage(
                        sender, Language.COMMAND_INVALID_NUMBER.with(FILL_TYPE.filled(Language.WORD_RADIUS))
                    );
                    return;
                }

                radius = inputRadius.get();
                if (radius < 1 || radius > 80) {
                    coins.getMessenger().sendMessage(sender, Language.COMMAND_INVALID_RANGE.with(
                        FILL_TYPE.filled(Language.WORD_RADIUS), FILL_MIN_1, FILL_MAX.filled(80)
                    ));
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

            coins.getScheduler().runEntityTaskLater(item, (long) (random * 5F), item::remove);
            amount++;
        }

        coins.getMessenger().sendMessage(sender, Language.REMOVE_REMOVED.with(FILL_AMOUNT.filled(amount)));
    }

    // todo add description as hover event
    // todo click event to suggest command
    private void handleSendHelp(CommandSender sender) {
        String currentVersion = coins.getVersionCheck().getPluginVersion();
        Optional<VersionPlugin> latestVersion = coins.getVersionCheck().getLatestVersion();

        if (coins.isDisabled()) {
            coins.getMessenger().sendHeader(sender, Language.WORD_DISABLED.getCapitalized());
        }
        else if (latestVersion.isPresent() && !latestVersion.get().getTag().equals(currentVersion) && Permissions.hasCommandVersion(sender)) {
            coins.getMessenger().sendHeader(sender, Language.WORD_OUTDATED.getCapitalized());
        }
        else {
            coins.getMessenger().sendHeader(sender, null);
        }

        int lines = 0;
        if (Config.ENABLE_WITHDRAW && Permissions.hasWithdraw(sender)) {
            coins.getMessenger().sendMessage(sender, Component.text("/withdraw <%s> [%s]".formatted(
                Language.WORD_VALUE, Language.WORD_AMOUNT
            ), ColorResolver.VAR));
            lines++;
        }
        if (Permissions.hasCommandDrop(sender)) {
            coins.getMessenger().sendMessage(sender, getDropCommand());
            lines++;
        }
        if (Permissions.hasCommandRemove(sender)) {
            coins.getMessenger().sendMessage(
                sender, Component.text("/coins remove [%s|all]".formatted(Language.WORD_RADIUS), ColorResolver.VAR)
            );
            lines++;
        }
        if (Permissions.hasCommandSettings(sender)) {
            coins.getMessenger().sendMessage(
                sender, Component.text("/coins settings [%s]".formatted(Language.WORD_PAGE), ColorResolver.VAR)
            );
            lines++;
        }
        if (Permissions.hasCommandReload(sender)) {
            coins.getMessenger().sendMessage(sender, Component.text("/coins reload", ColorResolver.VAR));
            lines++;
        }
        if (Permissions.hasCommandVersion(sender)) {
            coins.getMessenger().sendMessage(sender, Component.text("/coins version", ColorResolver.VAR));
            lines++;
        }
        if (Permissions.hasCommandToggle(sender)) {
            coins.getMessenger().sendMessage(sender, Component.text("/coins toggle", ColorResolver.VAR));
            lines++;
        }

        // if player has no permission for any of the commands
        if (lines == 0) {
            coins.getMessenger().sendMessage(sender, Component.text(coins.getDescription().getDescription(), ColorResolver.PRIMARY));
            coins.getMessenger().sendMessage(sender, coins.getSettings().getPluginUrl());
        }
    }

    private void dropCoins(Location location, int radius, int amount, double worth) {
        if (location.getWorld() == null) {
            return;
        }

        Location dropLocation = location.add(0.0, 0.5, 0.0);

        ItemStack coin;
        if (worth == 0) {
            coin = coins.getCreateCoin().createDropped();
        }
        else {
            coin = coins.getCreateCoin().createOther().setData(CoinMeta.COINS_WORTH, worth).build();
        }

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
