package me.justeli.coins.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Eli
 * @since February 10, 2022 (creation)
 */
public final class Permissions {
    private static boolean hasOldPermission(CommandSender sender, String outdated, String updated) {
        if (sender.hasPermission(outdated)) {
            Bukkit.getServer().getLogger().warning(
                "You are using the outdated permission '%s'. Please use '%s' instead. The used permission may be removed in the future.".formatted(outdated, updated)
            );
            return true;
        }

        return false;
    }

    // commands

    public static boolean hasCommandReload(CommandSender sender) {
        return sender.hasPermission("coins.command.reload") || hasOldPermission(sender, "coins.admin", "coins.command.reload");
    }

    public static boolean hasCommandSettings(CommandSender sender) {
        return sender.hasPermission("coins.command.settings") || hasOldPermission(sender, "coins.admin", "coins.command.settings");
    }

    public static boolean hasCommandLanguage(CommandSender sender) {
        return sender.hasPermission("coins.command.language") || hasOldPermission(sender, "coins.admin", "coins.command.language");
    }

    public static boolean hasCommandVersion(CommandSender sender) {
        return sender.hasPermission("coins.command.version") || hasOldPermission(sender, "coins.admin", "coins.command.version");
    }

    public static boolean hasCommandToggle(CommandSender sender) {
        return sender.hasPermission("coins.command.toggle") || hasOldPermission(sender, "coins.admin", "coins.command.toggle");
    }

    public static boolean hasCommandDrop(CommandSender sender) {
        return sender.hasPermission("coins.command.drop") || hasOldPermission(sender, "coins.drop", "coins.command.drop");
    }

    public static boolean hasCommandRemove(CommandSender sender) {
        return sender.hasPermission("coins.command.remove") || hasOldPermission(sender, "coins.remove", "coins.command.remove");
    }

    // withdraw

    public static boolean hasWithdraw(CommandSender sender) {
        return sender.hasPermission("coins.withdraw");
    }

    // bypasses

    public static boolean hasBypassLoseOnDeath(CommandSender sender) {
        return sender.hasPermission("coins.bypass.lose_on_death");
    }

    public static boolean hasBypassPercentageHit(CommandSender sender) {
        return sender.hasPermission("coins.bypass.percentage_hit");
    }

    public static boolean hasBypassLocationLimit(CommandSender sender) {
        return sender.hasPermission("coins.bypass.location_limit");
    }

    // drops

    public static boolean hasDropSpawnerMobs(CommandSender sender) {
        return sender.hasPermission("coins.drop.spawner_mobs") || hasOldPermission(sender, "coins.spawner", "coins.bypass.spawners");
    }

    public static boolean hasDropSplitMobs(CommandSender sender) {
        return sender.hasPermission("coins.drop.split_mobs");
    }

    public static boolean hasDropHostileMobs(@Nullable CommandSender sender) {
        return sender != null && sender.hasPermission("coins.drop.hostile_mobs");
    }

    public static boolean hasDropPassiveMobs(@Nullable CommandSender sender) {
        return sender != null && sender.hasPermission("coins.drop.passive_mobs");
    }

    public static boolean hasDropPlayers(@Nullable CommandSender sender) {
        return sender != null && sender.hasPermission("coins.drop.players");
    }

    // disabled

    public static boolean hasCoinsDisabled(CommandSender sender) {
        return sender.hasPermission("coins.disable") && !sender.isOp() && !sender.hasPermission("*");
    }

    // others

    private static final String MULTIPLIER_PREFIX = "coins.multiplier.";

    public static double getMultiplier(CommandSender sender) {
        List<Double> permissions = new ArrayList<>();
        for (PermissionAttachmentInfo permissionInfo : sender.getEffectivePermissions()) {
            String permission = permissionInfo.getPermission();
            if (permission.startsWith(MULTIPLIER_PREFIX)) {
                String number = permission.substring(MULTIPLIER_PREFIX.length());
                permissions.add(Util.parseDouble(number).orElse(1D));
            }
        }
        return permissions.isEmpty()? 1D : Collections.max(permissions);
    }
}
