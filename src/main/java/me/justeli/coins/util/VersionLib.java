package me.justeli.coins.util;

import org.bukkit.Bukkit;

import java.util.regex.Pattern;

/**
 * @author Eli
 * @since April 14, 2026
 */
public final class VersionLib {
    public enum Platform {
        BUKKIT,
        SPIGOT,
        PAPER
    }

    private static final Platform PLATFORM = findPlatform();
    private static final int VERSION = findVersion();

    public static boolean isPaper() {
        return PLATFORM == Platform.PAPER;
    }

    public static boolean isSpigot() {
        return PLATFORM == Platform.SPIGOT;
    }

    public static int getMinecraftVersion() {
        return VERSION;
    }

    public static Platform getPlatform() {
        return PLATFORM;
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static Platform findPlatform() {
        if (hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration")) {
            return Platform.PAPER;
        }
        else if (hasClass("org.spigotmc.SpigotConfig")) {
            return Platform.SPIGOT;
        }
        else {
            return Platform.BUKKIT;
        }
    }

    /// 1.19 -> 19, 26.1 -> 26
    private static int findVersion() {
        Pattern versionPattern = Pattern.compile("\\(\\w+: (\\d+)\\.(\\d+)\\.?(\\d+)?.*\\)");
        var matcher = versionPattern.matcher(Bukkit.getVersion());

        int version = 0;
        if (matcher.find()) {
            try {
                var matchResult = matcher.toMatchResult();
                int major = Integer.parseInt(matchResult.group(1), 10);
                version = (major == 1)? Integer.parseInt(matchResult.group(2), 10) : major;
            }
            catch (Exception ignored) {}
        }

        return version;
    }
}
