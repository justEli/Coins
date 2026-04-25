package me.justeli.coins.config;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.justeli.coins.Coins;
import me.justeli.coins.language.Entry;
import me.justeli.coins.language.Language;
import me.justeli.coins.language.LanguageEntry;
import me.justeli.coins.util.ColorResolver;
import me.justeli.coins.util.ComponentUtil;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * @author Eli
 * @since December 14, 2016 (creation); July 9, 2021 (rewrite)
 */
public final class Settings {
    private final Coins coins;
    private final Component pluginUrl;

    public Settings(Coins coins) {
        this.coins = coins;

        String website = coins.getDescription().getWebsite();
        if (website == null) {
            website = "https://www.spigotmc.org/resources/coins.33382/";
        }
        this.pluginUrl = Component.text(website, NamedTextColor.BLUE).clickEvent(ClickEvent.openUrl(website));

        createDefaultLocale();
        reload();
    }

    public static DecimalFormat DECIMAL_FORMATTER = new DecimalFormat();

    public Component getPluginUrl() {
        return pluginUrl;
    }

    /// reload all settings
    public void reload() {
        if (!coins.getDisabledReasons().isEmpty()) {
            coins.line(Level.SEVERE);
            coins.console(Level.SEVERE,
                "Plugin 'Coins' is disabled, until issues are fixed and the server is rebooted (see start-up log of Coins)."
            );
            coins.line(Level.SEVERE);
            return;
        }

        resetMultiplier();
        resetWarningCount();
        parseConfig();
        reloadLanguage();

        if (warnings != 0) {
            coins.console(Level.WARNING,
                "Loaded the config of Coins with %d warnings. Check above here for details.".formatted(warnings)
            );
        }
    }

    // config handling

    private FileConfiguration getOrSaveConfig() {
        File config = new File(coins.getDataFolder() + File.separator + "config.yml");
        if (!config.exists()) {
            coins.saveDefaultConfig();
        }

        return YamlConfiguration.loadConfiguration(config);
    }

    public static boolean USING_OLD_COLOR_CODES = false; // from before Coins v1.16 (April 2026)
    public static boolean USING_LEGACY_KEYS = false; // from before Coins v1.12 (Feb 2022)
    public static boolean USING_OLD_PLACEHOLDERS = false; // from a very old Coins version

    private static final Converter<String, String> LEGACY_CONVERTER =
        CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.LOWER_CAMEL);

    public void parseConfig() {
        FileConfiguration config = getOrSaveConfig();
        for (Field field : Config.class.getDeclaredFields()) {
            if (!field.isAnnotationPresent(ConfigEntry.class)) {
                continue;
            }

            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
            field.setAccessible(true);
            String configKey = configEntry.value();

            try {
                if (!config.contains(configKey)) {
                    String validKey = configKey;
                    configKey = LEGACY_CONVERTER.convert(validKey); // convert to old style
                    if (configKey != null && config.contains(configKey)) {
                        // deprecated in Coins v1.14 (April 2026)
                        showWarning("""
                            You are using the old format of config keys ('%s' instead of '%s'). Please update \
                            your config, as support for this will be dropped in the future."""
                            .formatted(configKey, validKey)
                        );
                        USING_LEGACY_KEYS = true;
                    }
                }

                if (configKey == null || !config.contains(configKey)) {
                    if (configEntry.required()) {
                        String prefixSuffix = field.getType() == String.class? "'" : "";
                        Object defaultValue = prefixSuffix + field.get(Config.class) + prefixSuffix;
                        showWarning("""
                            Config is missing `%s`. Using its default value '%s' now.%s Consider adding this to the config:
                            ----------------------------------------
                            %s: %s
                            ----------------------------------------"""
                            .formatted(
                                configKey,
                                defaultValue,
                                configEntry.motivation().isEmpty()? "" : " %s".formatted(configEntry.motivation()),
                                configEntry.value().replace(".", ":\n  "),
                                defaultValue
                            )
                        );
                    }
                    continue;
                }

                Class<?> configClass = field.getType();
                Object configValue;

                if (configClass == Set.class) {
                    List<String> stringList = config.getStringList(configKey);
                    configValue = new HashSet<>(stringList);
                }
                else if (configClass == Map.class) {
                    var section = config.getConfigurationSection(configKey);
                    if (section == null) {
                        continue;
                    }
                    Map<String, Object> map = section.getValues(false);
                    Map<String, Integer> configMap = new HashMap<>();

                    for (Map.Entry<String, Object> mapLoop : map.entrySet()) {
                        configMap.put(
                            mapLoop.getKey(),
                            Util.parseInt(mapLoop.getValue().toString()).orElse(1)
                        );
                    }
                    configValue = configMap;
                }
                else if (configClass == Component.class) {
                    String value = config.getString(configKey);
                    if (value == null) {
                        throw new NullPointerException();
                    }

                    // deprecated in Coins v1.16 (April 2026)
                    if (value.contains("{$}") || value.contains("%amount%")) {
                        USING_OLD_PLACEHOLDERS = true;
                        value = value.replace("{$}", "{currency}").replace("%amount%", "{amount}");
                        showWarning("""
                            Found outdated placeholders for '%s' in the config at `%s`. Change {$} or %%amount%% \
                            to {currency} or {amount}. Support will be removed in a future release."""
                            .formatted(value, configKey)
                        );
                    }

                    // deprecated in Coins v1.16 (April 2026)
                    if (ComponentUtil.isLegacyColored(value)) {
                        USING_OLD_COLOR_CODES = true;
                        String message = ComponentUtil.parseLegacyToMiniMessage(value);
                        showWarning("""
                            Found outdated color codes for '%s' in the config at `%s`. Change this to minimessage \
                            formatting '%s'. Support for old color codes will be removed in a future release. More info: \
                            https://github.com/justEli/Coins/wiki/Formatting-messages-and-components"""
                            .formatted(value, configKey, message)
                        );
                        configValue = ComponentUtil.parse(message);
                    }
                    else {
                        configValue = ComponentUtil.parse(value);
                    }
                }
                else if (configClass == String.class || configClass == Material.class || configClass == SoundKey.class || configClass == MessagePosition.class) {
                    String value = config.getString(configKey);
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    else if (configClass == Material.class) {
                        configValue = getMaterial(value, configEntry.value()).orElse(Material.SUNFLOWER);
                    }
                    else if (configClass == SoundKey.class) {
                        configValue = getSound(value, configEntry.value()).orElse(new SoundKey("minecraft:item.armor.equip_gold"));
                    }
                    else if (configClass == MessagePosition.class) {
                        Optional<MessagePosition> position = getMessagePosition(value, configEntry.value());
                        if (position.isPresent()) {
                            configValue = position.get();
                        }
                        else {
                            continue;
                        }
                    }
                    else {
                        configValue = value;
                    }
                }
                else if (configClass == Long.class || configClass == Integer.class || configClass == Float.class || configClass == Double.class) {
                    double value = Double.parseDouble(config.get(configKey, "0").toString());

                    if (configClass == Long.class) {
                        configValue = (long) value;
                    }
                    else if (configClass == Integer.class) {
                        configValue = (int) value;
                    }
                    else if (configClass == Float.class) {
                        configValue = (float) value;
                    }
                    else {
                        configValue = value;
                    }
                }
                else {
                    configValue = configClass.cast(config.get(configKey));
                }

                field.set(Config.class, configValue);
            }
            catch (Exception exception) {
                try {
                    Object defaultValue = field.get(Config.class);
                    showWarning("""
                        Config file has wrong value at `%s`. Using its default value now (%s)."""
                        .formatted(configEntry.value(), defaultValue)
                    );
                }
                catch (IllegalAccessException ignored) {}
            }
        }

        parseRemainingOptions();
        coins.console(Level.INFO, "Settings from 'config.yml' have been loaded.");
    }

    private void parseRemainingOptions() {
        Config.BLOCK_DROPS.clear();
        Config.RAW_BLOCK_DROPS.forEach((k, v) -> {
            Optional<Material> material = getMaterial(k, "block-drops");
            material.ifPresent(value -> Config.BLOCK_DROPS.put(value, v));
        });

        Config.MOB_MULTIPLIER.clear();
        Config.RAW_MOB_MULTIPLIER.forEach((k, v) -> {
            Optional<EntityType> entityType = getEntityType(k, "mob-multiplier");
            entityType.ifPresent(type -> Config.MOB_MULTIPLIER.put(type, v));
        });

        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
        if (Config.DIGIT_DECIMAL_SEPARATOR.length() == 1) {
            formatSymbols.setDecimalSeparator(Config.DIGIT_DECIMAL_SEPARATOR.charAt(0));
        }
        if (Config.DIGIT_GROUP_SEPARATOR.length() == 1) {
            formatSymbols.setGroupingSeparator(Config.DIGIT_GROUP_SEPARATOR.charAt(0));
        }

        String decimals = Config.MONEY_DECIMALS == 0? "#" : "0".repeat(Config.MONEY_DECIMALS);
        String groupSeparator = Config.DIGIT_GROUP_SEPARATOR.isEmpty()? "" : ",";

        DECIMAL_FORMATTER = new DecimalFormat("#" + groupSeparator + "##0." + decimals, formatSymbols);
    }

    private Optional<Material> getMaterial(String name, String configKey) {
        var key = NamespacedKey.fromString(name);
        if (key != null) {
            var type = Registry.MATERIAL.get(key);
            if (type != null) {
                return Optional.of(type);
            }
        }

        Material material = Material.matchMaterial(name.replace(" ", "_").toUpperCase().replace("COIN", "SUNFLOWER"));
        if (material != null) {
            // deprecated in Coins v1.16 (April 2026)
            showWarning("""
                Found an outdated material '%s' in the config at `%s`. Change this to its namespaced key '%s'. \
                Support for outdated material types will be removed in a future release."""
                .formatted(name, configKey, material.getKey().toString())
            );
            return Optional.of(material);
        }

        showWarning("""
            The material '%s' in the config at `%s` does not exist. \
            Please use a namespaced material, as from the suggestions of the /give command."""
            .formatted(name, configKey)
        );
        return Optional.empty();
    }

    private Optional<MessagePosition> getMessagePosition(String name, String configKey) {
        try {
            return Optional.of(MessagePosition.valueOf(name.replace(" ", "_").toUpperCase()));
        }
        catch (IllegalArgumentException exception) {
            showWarning("""
                Message position '%s' in the config at `%s` is invalid. \
                Use either 'actionbar', 'title', 'subtitle', or 'chat'."""
                .formatted(name, configKey)
            );
            return Optional.empty();
        }
    }

    private Optional<EntityType> getEntityType(String name, String configKey) {
        var key = NamespacedKey.fromString(name);
        if (key != null) {
            var type = Registry.ENTITY_TYPE.get(key);
            if (type != null) {
                return Optional.of(type);
            }
        }

        try {
            // deprecated in Coins v1.16 (April 2026)
            EntityType type = EntityType.valueOf(name.replace(" ", "_").toUpperCase());
            showWarning("""
                Found an outdated entity type '%s' in the config at `%s`. Change this to its namespaced key '%s'. \
                Support for outdated entity types will be removed in a future release."""
                .formatted(name, configKey, type.getKey().toString())
            );
            return Optional.of(type);
        }
        catch (IllegalArgumentException exception) {
            showWarning("""
                The entity type '%s' in the config at `%s` does not exist. \
                Please use a namespaced entity type, as from the suggestions of the /summon command."""
                .formatted(name, configKey)
            );
            return Optional.empty();
        }
    }

    private Optional<SoundKey> getSound(String name, String configKey) {
        // outdated way of parsing sound
        var sound = fromEnumSound(name);
        if (sound.isPresent()) {
            try {
                showWarning("""
                    Found an outdated sound type '%s' in the config at `%s`. Change this to its namespaced key '%s'. \
                    Support for outdated sound types will be removed in a future release."""
                    .formatted(name, configKey, sound.get().getKey().toString())
                );
                return Optional.of(new SoundKey(sound.get()));
            }
            catch (Throwable ignored) {}
        }

        var key = NamespacedKey.fromString(name);
        if (key != null) {
            return Optional.of(new SoundKey(key));
        }

        showWarning("""
            The sound '%s' in the config at `%s` is not a valid namespaced sound type. Please use a namespaced sound, \
            as from the suggestions of the /playsound command. This can also be a custom sound."""
            .formatted(name, configKey)
        );
        return Optional.empty();
    }

    // parse Sound from enum, will be removed in the future
    // deprecated in Coins v1.15 (April 2026)
    private static Optional<Sound> fromEnumSound(String name) {
        try {
            var sound = Sound.valueOf(name.toUpperCase().replace(" ", "_"));
            var key = NamespacedKey.fromString(name);
            if (key == null) {
                return Optional.of(sound); // invalid key, so must be from enum
            }

            if (Registry.SOUNDS.get(key) != null) {
                return Optional.empty(); // valid key in the registry, so not from enum
            }
            return Optional.of(sound);
        }
        catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static final Converter<String, String> VAR_CONVERTER =
        CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_HYPHEN);

    public List<Component> getConfigKeys() {
        Map<String, Component> values = new TreeMap<>();
        for (Field field : Config.class.getDeclaredFields()) {
            boolean staticAndPublic = Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers());
            if (!staticAndPublic || field.isAnnotationPresent(Deprecated.class)) {
                continue;
            }

            var name = VAR_CONVERTER.convert(field.getName());
            if (name == null) {
                continue;
            }

            values.put(field.getName(),
                Component.text()
                    .append(Component.text(name))
                    .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                    .append(getConfigValue(field))
                    .build()
            );
        }

        return new ArrayList<>(values.values());
    }

    private static Component getConfigValue(Field field) {
        try {
            Object value = field.get(Config.class);
            return switch (value) {
                case Component component0 -> ComponentUtil.replaceCurrency(component0);
                case Boolean bool -> Component.text(bool, bool? NamedTextColor.GREEN : NamedTextColor.RED);
                case Keyed keyed -> Component.text(keyed.getKey().toString(), ColorResolver.VAR);
                default -> ComponentUtil.replaceCurrency(Component.text(value.toString(), ColorResolver.VAR));
            };
        }
        catch (IllegalArgumentException | IllegalAccessException exception) {
            return Component.text("Unknown", NamedTextColor.GRAY);
        }
    }

    // language handling

    // - nl-NL  2026/04
    // - es-ES  2026/04  discord=xxdaterxx.337196910194589696
    // - it-IT  2026/04  spigot=peppe73.693388

    private static final String DEFAULT_LOCALE = "en-US";
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");

    public void reloadLanguage() {
        // check if 'locale' is configured correctly
        if (!LOCALE_PATTERN.matcher(Config.LOCALE).matches()) {
            String corrected = toValidLocale(Config.LOCALE);
            if (corrected.isEmpty()) {
                showWarning("""
                    Found an incorrect locale in the config. Now using the default locale '%s'. \
                    Please use a locale from the 'locale' folder in Coins, or create your own in the format 'xx-YY'."""
                    .formatted(DEFAULT_LOCALE)
                );
                Config.LOCALE = DEFAULT_LOCALE;
            }
            else {
                showWarning("""
                    Found an invalid locale '%s' in the config at `locale`. Change this to '%s'."""
                    .formatted(Config.LOCALE, corrected)
                );
                Config.LOCALE = corrected;
            }
        }

        Coins.EXECUTOR.submit(this::downloadLanguageFiles);

        try { parseLanguage(); }
        catch (Throwable throwable) {
            coins.console(Level.WARNING, "Could not load language file for '%s'.".formatted(Config.LOCALE));
        }
    }

    private void downloadLanguageFiles() {
        List<String> downloadedLocales = new ArrayList<>();
        try (var client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
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
                    Consider to submit your language file to https://plugin.coins.community/discord, so \
                    it can become a supported language."""
                );
            }
        }
        catch (IOException | InterruptedException ignored) {
            // download failed, but that's ok
        }

        if (!downloadedLocales.isEmpty()) {
            coins.console(Level.INFO, """
                New language files have been added the 'locale' folder. These locale(s) can now be used in the config: '%s'"""
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
            showWarning("""
                Language '%s' is no longer supported. Please add `locale: 'en-US'` to the config. Language \
                handling has been completely rewritten, sorry for the inconvenience. Please see the 'locale' folder \
                of the plugin to find locales to use, or create your own.""".formatted(Config.LANGUAGE)
            );
        }

        createLocale(Config.LOCALE, true);
        coins.console(Level.INFO, "Language entries from 'locale/%s.json' have been loaded.".formatted(Config.LOCALE));
    }

    // warnings in settings

    private int warnings = 0;

    public void showWarning(String message) {
        warnings++;
        coins.console(Level.WARNING, "#" + warnings + ": " + message);
    }

    public void resetWarningCount() {
        this.warnings = 0;
    }

    public int getWarningCount() {
        return warnings;
    }

    // coin multipliers

    private final Map<UUID, Double> playerMultiplier = new ConcurrentHashMap<>();

    public void resetMultiplier(Player player) {
        playerMultiplier.remove(player.getUniqueId());
    }

    public void resetMultiplier() {
        playerMultiplier.clear();
    }

    public double getMultiplier(Player player) {
        if (!playerMultiplier.containsKey(player.getUniqueId())) {
            playerMultiplier.put(player.getUniqueId(), Permissions.getMultiplier(player));
        }
        return playerMultiplier.computeIfAbsent(player.getUniqueId(), empty -> 1D);
    }
}
