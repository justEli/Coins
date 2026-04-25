package me.justeli.coins.util;

import com.google.gson.JsonParser;
import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.logging.Level;

/**
 * @author Eli
 * @since February 04, 2022 (creation)
 */
public final class VersionCheck {
    private final Coins coins;
    private final String pluginVersion;

    public VersionCheck(Coins coins) {
        this.coins = coins;
        if (VersionUtil.isPlatformAtLeast(VersionUtil.Platform.PAPER)) {
            this.pluginVersion = coins.getPluginMeta().getVersion();
        }
        else {
            this.pluginVersion = coins.getDescription().getVersion();
        }
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void checkVersion() {
        Optional<VersionPlugin> version = retrieveLatestVersion("JustEli/Coins");
        if (version.isEmpty()) {
            return;
        }

        this.latestVersion = version.get();
        if (!Config.CHECK_FOR_UPDATES) {
            return;
        }

        if (!pluginVersion.equals(latestVersion.getTag()) && !latestVersion.isPreRelease()) {
            coins.line(Level.WARNING);
            coins.console(Level.WARNING, "  Detected an outdated version of Coins (%s is installed).".formatted(pluginVersion));
            coins.console(Level.WARNING, "  The latest version is %s, released on %s.".formatted(
                latestVersion.getTag(),
                Util.formatDate(latestVersion.getTime())
            ));
            coins.console(Level.WARNING, "  Download: %s".formatted(coins.getDescription().getWebsite()));
            coins.line(Level.WARNING);
        }
    }

    private VersionPlugin latestVersion;
    public Optional<VersionPlugin> getLatestVersion() {
        return Optional.ofNullable(latestVersion);
    }

    public static Optional<VersionPlugin> retrieveLatestVersion(String repository) {
        try {
            URL url = URI.create("https://api.github.com/repos/" + repository + "/releases/latest").toURL();
            URLConnection request = url.openConnection();

            request.setReadTimeout(1000);
            request.setConnectTimeout(1000);
            request.connect();

            try (var reader = new InputStreamReader((InputStream) request.getContent())) {
                var root = JsonParser.parseReader(reader);
                var jsonObject = root.getAsJsonObject();
                return Optional.of(new VersionPlugin(
                    jsonObject.get("tag_name").getAsString(),
                    jsonObject.get("prerelease").getAsBoolean(),
                    jsonObject.get("name").getAsString(),
                    jsonObject.get("published_at").getAsString()
                ));
            }
        }
        catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
