package me.justeli.coins.util;

import me.justeli.coins.config.Config;
import me.justeli.coins.config.Settings;
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
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * @author Eli
 * @since January 6, 2020 (creation)
 */
public final class Util {
    private static final SplittableRandom RANDOM = new SplittableRandom();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d, yyyy");

    public static String formatDate(long millis) {
        return DATE_FORMAT.format(new Date(millis));
    }

    public static String toCapitalized(String message) {
        return (message == null || message.isEmpty())? "" : message.substring(0, 1).toUpperCase() + message.substring(1).toLowerCase();
    }

    public static boolean isHostile(Entity entity) {
        return entity instanceof Monster
            || entity instanceof Flying
            || entity instanceof Slime
            || (entity instanceof Golem && !(entity instanceof Snowman))
            || (entity instanceof Wolf && ((Wolf) entity).isAngry())
            || entity instanceof Boss;
    }

    public static boolean isPassive(Entity entity) {
        return !isHostile(entity)
            && !(entity instanceof Player)
            && entity instanceof LivingEntity
            && !(entity instanceof ArmorStand);
    }

    public static Optional<Player> getOnlinePlayer(String incomplete) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            String lowercaseName = player.getName().toLowerCase();
            String lowercaseIncomplete = incomplete.toLowerCase();
            if (lowercaseName.startsWith(lowercaseIncomplete)) {
                return Optional.of(player);
            }
            else if (player.getDisplayName().toLowerCase().contains(lowercaseIncomplete)) {
                return Optional.of(player);
            }
            else if (lowercaseName.contains(lowercaseIncomplete)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    // todo move to handler/action class together with dropCoins and depositMoney
    public static void playCoinPickupSound(Player player) {
        float volume = Config.SOUND_VOLUME;
        float pitch = Config.SOUND_PITCH;

        player.playSound(
            player.getEyeLocation(),
            Config.SOUND_NAME.toString(),
            volume <= 0? .3F : volume,
            pitch <= 0? .3F : pitch
        );
    }

    public static boolean isDisabledHere(@Nullable World world) {
        if (world == null) {
            return true;
        }

        return Config.DISABLED_WORLDS.contains(world.getName());
    }

    public static double getRandomMoneyAmount() {
        double second = Config.MONEY_AMOUNT_FROM;
        double first = Config.MONEY_AMOUNT_TO - second;

        return RANDOM.nextDouble() * first + second;
    }

    public static double getRandomTakeAmount() {
        double second = Config.MONEY_TAKEN_FROM;
        double first = Config.MONEY_TAKEN_TO - second;

        return RANDOM.nextDouble() * first + second;
    }

    public static double toRoundedMoneyDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(Config.MONEY_DECIMALS, RoundingMode.HALF_UP).doubleValue();
    }

    public static String toFormattedMoneyDecimals(double input) {
        return Settings.DECIMAL_FORMATTER.format(toRoundedMoneyDecimals(input));
    }

    public static Optional<Integer> parseInt(String number) {
        try {
            return Optional.of(Integer.parseInt(number.replaceAll("[<>\\[\\]]", "")));
        }
        catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Optional<Double> parseDouble(String number) {
        try {
            return Optional.of(toRoundedMoneyDecimals(Double.parseDouble(number.replaceAll("[<>\\[\\]]", ""))));
        }
        catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Optional<Player> getRootDamage(LivingEntity dead) {
        if (dead.getKiller() != null) {
            return Optional.of(dead.getKiller());
        }

        EntityDamageEvent damageCause = dead.getLastDamageCause();
        if (damageCause instanceof EntityDamageByEntityEvent) {
            return getRootDamage((EntityDamageByEntityEvent) damageCause);
        }

        return Optional.empty();
    }

    public static Optional<Player> getRootDamage(EntityDamageByEntityEvent damageEvent) {
        Entity attacker = damageEvent.getDamager();
        if (attacker instanceof Player player) {
            return Optional.of(player);
        }

        if (!(attacker instanceof Projectile projectile)) {
            return Optional.empty();
        }

        ProjectileSource shooter = projectile.getShooter();
        if (shooter instanceof Player player) {
            return Optional.of(player);
        }

        return Optional.empty();
    }
}
