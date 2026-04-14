package me.justeli.coins.util;

import me.justeli.coins.config.Config;
import me.justeli.coins.config.MessagePosition;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Eli @ January 6, 2020 (creation) */
public final class Util
{
    private static final Pattern HEX_PATTERN = Pattern.compile("(?<!\\\\)(&#[a-fA-F\\d]{6})");
    private static final HashMap<UUID, Double> PLAYER_MULTIPLIER = new HashMap<>();
    private static final SplittableRandom RANDOM = new SplittableRandom();

    public static String color (@NotNull String msg)
    {
        return ChatColor.translateAlternateColorCodes('&', parseRGB(msg));
    }

    public static String formatAmountAndCurrency (String text, double amount)
    {
        String displayAmount = doubleToString(amount);
        return formatCurrency(text.replaceAll("(%amount%|\\{amount})", Matcher.quoteReplacement(displayAmount)));
    }

    public static String formatCurrency (String text)
    {
        // {currency} or {$}
        return text.replaceAll("(\\{currency}|\\{\\$})", Matcher.quoteReplacement(Config.CURRENCY_SYMBOL));
    }

    private static String parseRGB (@NotNull String msg)
    {
        Matcher matcher = HEX_PATTERN.matcher(msg);
        while (matcher.find())
        {
            String color = msg.substring(matcher.start(), matcher.end());
            String hex = color.replace("&", "").toUpperCase();

            msg = msg.replace(color, ChatColor.of(hex).toString());
            matcher = HEX_PATTERN.matcher(msg);
        }

        return msg;
    }

    public static void resetMultiplier (Player player)
    {
        PLAYER_MULTIPLIER.remove(player.getUniqueId());
    }

    public static void resetMultiplier ()
    {
        PLAYER_MULTIPLIER.clear();
    }

    public static double getMultiplier (Player player)
    {
        if (!PLAYER_MULTIPLIER.containsKey(player.getUniqueId()))
        {
            List<Double> permissions = new ArrayList<>();
            for (PermissionAttachmentInfo permissionInfo : player.getEffectivePermissions())
            {
                String permission = permissionInfo.getPermission();
                if (permission.startsWith(PermissionNode.MULTIPLIER_PREFIX))
                {
                    permissions.add(PermissionNode.multiplierFromPermission(permission));
                }
            }
            PLAYER_MULTIPLIER.put(player.getUniqueId(), permissions.isEmpty()? 1D : Collections.max(permissions));
        }
        return PLAYER_MULTIPLIER.computeIfAbsent(player.getUniqueId(), empty -> 1D);
    }

    public static boolean isHostile (Entity entity)
    {
        return entity instanceof Monster
            || entity instanceof Flying
            || entity instanceof Slime
            || (entity instanceof Golem && !(entity instanceof Snowman))
            || (entity instanceof Wolf && ((Wolf) entity).isAngry())
            || entity instanceof Boss;
    }

    public static boolean isPassive (Entity entity)
    {
        return !isHostile(entity)
            && !(entity instanceof Player)
            && entity instanceof LivingEntity
            && !(entity instanceof ArmorStand);
    }

    public static Player getOnlinePlayer (String incomplete)
    {
        for (Player player : Bukkit.getServer().getOnlinePlayers())
        {
            if (player.getName().toLowerCase().startsWith(incomplete.toLowerCase()))
            {
                return player;
            }
            else if (player.getDisplayName().toLowerCase().contains(incomplete.toLowerCase()))
            {
                return player;
            }
            else if (player.getName().toLowerCase().contains(incomplete.toLowerCase()))
            {
                return player;
            }
        }
        return null;
    }

    public static void playCoinPickupSound (Player player)
    {
        float volume = Config.SOUND_VOLUME;
        float pitch = Config.SOUND_PITCH;

        player.playSound(player.getEyeLocation(), Config.SOUND_NAME, volume == 0? 0.3f : volume, pitch == 0? 0.3f : pitch);
    }

    public static boolean isDisabledHere (@Nullable World world)
    {
        if (world == null)
            return true;

        return Config.DISABLED_WORLDS.contains(world.getName());
    }

    public static double getRandomMoneyAmount ()
    {
        double second = Config.MONEY_AMOUNT_FROM;
        double first = Config.MONEY_AMOUNT_TO - second;

        return RANDOM.nextDouble() * first + second;
    }

    public static double getRandomTakeAmount ()
    {
        double second = Config.MONEY_TAKEN_FROM;
        double first = Config.MONEY_TAKEN_TO - second;

        return RANDOM.nextDouble() * first + second;
    }

    public static double round (double value)
    {
        return BigDecimal.valueOf(value).setScale(Config.MONEY_DECIMALS, RoundingMode.HALF_UP).doubleValue();
    }

    public static String doubleToString (double input)
    {
        return Config.DECIMAL_FORMATTER.format(round(input));
    }

    public static Optional<Integer> parseInt (String arg)
    {
        try { return Optional.of(Integer.parseInt(arg)); }
        catch (NumberFormatException exception) { return Optional.empty(); }
    }

    public static Optional<Double> parseDouble (String arg)
    {
        try { return Optional.of(Util.round(Double.parseDouble(arg))); }
        catch (NumberFormatException exception) { return Optional.empty(); }
    }

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy");

    // page starts at 1
    public static ArrayList<String> page (ArrayList<String> items, int pageSize, int pageNumber)
    {
        if (pageNumber <= 0)
        {
            return new ArrayList<>();
        }

        ArrayList<String> pages = new ArrayList<>();
        for (int i = (pageNumber - 1) * pageSize; i < pageNumber * pageSize; i++)
        {
            if (items.size() <= i)
                continue;

            pages.add(items.get(i));
        }

        return pages;
    }

    public static Optional<Player> getRootDamage (LivingEntity dead)
    {
        if (dead.getKiller() != null)
        {
            return Optional.of(dead.getKiller());
        }

        EntityDamageEvent damageCause = dead.getLastDamageCause();
        if (damageCause instanceof EntityDamageByEntityEvent)
        {
            return getRootDamage((EntityDamageByEntityEvent) damageCause);
        }

        return Optional.empty();
    }

    public static Optional<Player> getRootDamage (EntityDamageByEntityEvent damageEvent)
    {
        Entity attacker = damageEvent.getDamager();
        if (attacker instanceof Player)
        {
            return Optional.of((Player) attacker);
        }

        if (!(attacker instanceof Projectile))
        {
            return Optional.empty();
        }

        ProjectileSource shooter = ((Projectile) attacker).getShooter();
        if (shooter instanceof Player)
        {
            return Optional.of((Player) shooter);
        }

        return Optional.empty();
    }

    public static void send (MessagePosition position, Player player, String message, double amount)
    {
        switch (position)
        {
            case ACTIONBAR -> new ActionBar(message, amount).send(player);
            case TITLE -> player.sendTitle(Util.color(Util.formatAmountAndCurrency(message, amount)), ChatColor.RESET.toString(), 10, 60, 10);
            case SUBTITLE -> SubTitle.of(Util.formatAmountAndCurrency(message, amount)).send(player);
            case CHAT -> player.sendMessage(Util.color(Util.formatAmountAndCurrency(message, amount)));
        }
    }
}
