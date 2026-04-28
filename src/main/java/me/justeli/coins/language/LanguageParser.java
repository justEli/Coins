package me.justeli.coins.language;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.justeli.coins.Coins;
import me.justeli.coins.config.Config;
import me.justeli.coins.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * @author Eli
 * @since April 27, 2026
 */
public final class LanguageParser {
    private final Coins coins;
    private final Settings settings;

    public LanguageParser(Coins coins, Settings settings) {
        this.coins = coins;
        this.settings = settings;
        createDefaultLocale();
    }

    // - nl-NL  2026/04
    // - es-ES  2026/04  discord=xxdaterxx.337196910194589696
    // - it-IT  2026/04  spigot=peppe73.693388
    // - sv-SE  2026/04  discord=k4rlus.221730501315002368
    // - zh-CN  2026/04  spigot=chenxuuu.325888, github=chenxuuu
    // - he-IL  2026/04  discord=leom3565.768085724410413096
    // - ru-RU  2026/04  spigot=allfire0.1632755, discord=allf1re.1135600549283246220

    private static final String DEFAULT_LOCALE = "en-US";
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");

    private static final ExecutorService SINGLE_EXECUTOR = Executors.newSingleThreadExecutor();

    public void reloadLanguage() {
        // check if 'locale' is configured correctly
        if (!LOCALE_PATTERN.matcher(Config.LOCALE).matches()) {
            String corrected = toValidLocale(Config.LOCALE);
            if (corrected.isEmpty()) {
                settings.showWarning("""
                    Found an incorrect locale in the config. Now using the default locale '%s'. \
                    Please use a locale from the 'locale' folder in Coins, or create your own in the format 'xx-YY'."""
                    .formatted(DEFAULT_LOCALE)
                );
                Config.LOCALE = DEFAULT_LOCALE;
            }
            else {
                settings.showWarning("""
                    Found an invalid locale '%s' in the config at `locale`. Change this to '%s'."""
                    .formatted(Config.LOCALE, corrected)
                );
                Config.LOCALE = corrected;
            }
        }

        SINGLE_EXECUTOR.submit(this::downloadLanguageFiles);

        try { parseLanguage(); }
        catch (Throwable throwable) {
            coins.console(Level.WARNING, "Could not load language file for '%s'.".formatted(Config.LOCALE));
        }
    }

    private void downloadLanguageFiles() {
        List<String> downloadedLocales = new ArrayList<>();
        try (var client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(URI.create("https://api.github.com/repos/justEli/Coins/contents/locale"))
                .header("Accept", "application/vnd.github+json").build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();

            Path directory = coins.getDataFolder().toPath().resolve("locale");
            Files.createDirectories(directory); // always create directory

            Set<String> supportedLocales = new TreeSet<>();
            // every file from locale in GitHub
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                String type = json.get("type").getAsString();
                String name = json.get("name").getAsString();

                if (!"file".equals(type) || !name.endsWith(".json")) {
                    continue; // only allow .json in root folder
                }

                String locale = name.substring(0, name.length() - 5); // trim .json
                supportedLocales.add(locale);

                if (name.equals(DEFAULT_LOCALE + ".json")) {
                    continue; // included by default
                }

                Path target = directory.resolve(name);
                if (Files.exists(target)) {
                    continue; // skip when file already exists
                }

                HttpRequest fileRequest = HttpRequest.newBuilder()
                    .timeout(Duration.ofSeconds(10))
                    .uri(URI.create(json.get("download_url").getAsString()))
                    .build();

                HttpResponse<String> fileResponse = client.send(fileRequest, HttpResponse.BodyHandlers.ofString());
                if (fileResponse.statusCode() == 200) {
                    Files.writeString(target, fileResponse.body()); // save language file
                    downloadedLocales.add(locale);
                }
            }

            if (!supportedLocales.contains(Config.LOCALE)) {
                coins.console(Level.INFO, """
                    Consider to submit your language file to https://plugin.coins.community/discord for it \
                    to become a supported language for other servers."""
                );
            }
        }
        catch (IOException | InterruptedException ignored) {
            // download failed, but that's ok
        }

        if (!downloadedLocales.isEmpty()) {
            coins.console(Level.INFO, """
                Language files have been added to the 'locale' folder, and can be used in the config: '%s'"""
                .formatted(String.join("', '", downloadedLocales))
            );
        }
    }

    private static @NotNull String toValidLocale(@NotNull String locale) {
        var parts = locale.split("[ -_]");
        if (parts.length == 1) {
            String modified = parts[0].toLowerCase();
            if (LOCALE_PATTERN.matcher(modified).matches()) {
                return modified;
            }
        }
        else if (parts.length == 2) {
            String modified = parts[0].toLowerCase() + "-" + parts[1].toUpperCase();
            if (LOCALE_PATTERN.matcher(modified).matches()) {
                return modified;
            }
        }

        return "";
    }

    // only ran on startup to create locale/en-US.json (and add new entries if needed)
    private void createDefaultLocale() {
        try {
            createLocale(DEFAULT_LOCALE, false);
        }
        catch (IOException exception) {
            coins.console(Level.WARNING, "Unable to create the default language file for '%s'.".formatted(DEFAULT_LOCALE));
        }
    }

    private static final Gson GSON_WRITER = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private void createLocale(String locale, boolean alsoLoadToCache) throws IOException {
        // load or create language file
        Path localeFile = coins.getDataFolder().toPath().resolve("locale").resolve(locale + ".json");
        Files.createDirectories(localeFile.getParent()); // always create locale directory

        JsonObject json;
        if (Files.exists(localeFile)) {
            // load current json entries from the language file
            try (Reader reader = Files.newBufferedReader(localeFile)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
        else {
            coins.console(Level.INFO, """
                Language file doesn't exist for 'locale/%s.json'. Creating one with default values.""".formatted(locale)
            );
            json = new JsonObject();
        }

        // go over all the language keys
        for (Field field : Language.class.getDeclaredFields()) {
            if (!field.isAnnotationPresent(LanguageEntry.class)) {
                continue;
            }

            try {
                LanguageEntry languageEntry = field.getAnnotation(LanguageEntry.class);
                field.setAccessible(true);
                String languageKey = languageEntry.value(); // the json language key

                var value = field.get(Language.class); // get the value from the field
                if (value instanceof Entry entry) {
                    if (json.has(languageKey)) {
                        if (alsoLoadToCache) {
                            // key exists in the file; cache it to Language
                            Method method = field.getType().getMethod("of", String.class);
                            field.set(Language.class, method.invoke(Language.class, json.get(languageKey).getAsString()));
                        }
                    }
                    else {
                        // key doesn't exist; add this value to the json file
                        json.addProperty(languageKey, entry.toString());
                    }
                }
            }
            catch (Exception ignored) {
                // cannot handle language key, but that's ok
            }
        }

        // write the added keys to json file
        try (Writer writer = Files.newBufferedWriter(localeFile)) {
            GSON_WRITER.toJson(json, writer);
        }
    }

    private void parseLanguage() throws IOException {
        if (!Config.LANGUAGE.isEmpty()) {
            // support dropped since Coins v1.16 (April 2026)
            settings.showWarning("""
                Language '%s' is no longer supported. Please add `locale: 'en-US'` to the config, and remove `language`. \
                Language handling has been completely rewritten, sorry for the inconvenience. Please see the 'locale' folder \
                of the plugin to find locales to use, or create your own.""".formatted(Config.LANGUAGE)
            );
            Settings.MIGRATED_TO_LOCALE = false;
        }

        createLocale(Config.LOCALE, true);
        coins.console(Level.INFO, "Language from 'locale/%s.json' has been loaded.".formatted(Config.LOCALE));
    }
}
