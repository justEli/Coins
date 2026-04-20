package me.justeli.coins.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;

/**
 * @author Eli
 * @since February 04, 2022 (creation)
 */
public final class PluginVersionUtil {
    private final Coins coins;
    public PluginVersionUtil(Coins coins) {
        this.coins = coins;
    }

    public void checkVersion() {
        Optional<PluginVersion> version = retrieveLatestVersion("JustEli/Coins");
        if (version.isEmpty()) {
            return;
        }

        this.latestVersion = version.get();
        if (!Config.CHECK_FOR_UPDATES) {
            return;
        }

        String currentVersion = coins.getDescription().getVersion();
        if (!currentVersion.equals(latestVersion.tag()) && !latestVersion.preRelease()) {
            coins.line(Level.WARNING);
            coins.console(Level.WARNING, "  Detected an outdated version of Coins (%s is installed).".formatted(
                currentVersion
            ));
            coins.console(Level.WARNING, "  The latest version is %s, released on %s.".formatted(
                latestVersion.tag(),
                Util.DATE_FORMAT.format(new Date(latestVersion.time()))
            ));
            coins.console(Level.WARNING, "  Download: %s".formatted(coins.getDescription().getWebsite()));
            coins.line(Level.WARNING);
        }
    }

    private PluginVersion latestVersion;
    public Optional<PluginVersion> getLatestVersion() {
        return Optional.ofNullable(latestVersion);
    }

    public static Optional<PluginVersion> retrieveLatestVersion(String repository) {
        try {
            URL url = new URL("https://api.github.com/repos/" + repository + "/releases/latest");
            URLConnection request = url.openConnection();

            request.setReadTimeout(1000);
            request.setConnectTimeout(1000);
            request.connect();

            try (var reader = new InputStreamReader((InputStream) request.getContent())) {
                JsonElement root = JsonParser.parseReader(reader);
                JsonObject jsonObject = root.getAsJsonObject();
                return Optional.of(new PluginVersion(
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
