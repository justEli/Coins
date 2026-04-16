package me.justeli.coins.config;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import me.justeli.coins.Coins;
import me.justeli.coins.util.Permissions;
import me.justeli.coins.util.Util;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author Eli
 * @since December 14, 2016 (creation); July 9, 2021 (rewrite)
 */
public final class Settings {
    private final Coins coins;
    private final JSONObject fallbackLanguage;

    public Settings(Coins coins) {
        this.coins = coins;
        this.fallbackLanguage = retrieveFallbackLanguage();
        reload();
    }

    private static final List<String> LANGUAGES = List.of(
        "english",
        "dutch",
        "spanish",      // 2017/4 (spigot=robi)
        "swedish",      // 2022/10 (discord=k4rlus.221730501315002368)
        "german",       // 2017/5
        "chinese",      // 2022/6 (github=chenxuuu)
        "hungarian",    // 2017/12 (github=montlikadani)
        "french",       // 2019/8 (spigot=mvez73)
        "italian",      // 2020/10 (spigot=Peppe73)
        "russian",      // 2022/3 (discord=BeastMark.932023237313962054)
        "turkish"       // 2022/8 (discord=Purpely.710585615841886260)
    );

    public void reloadLanguage() {
        for (String language : LANGUAGES) {
            if (!new File(coins.getDataFolder() + File.separator + "language" + File.separator + language + ".json").exists()) {
                coins.saveResource("language/" + language + ".json", false);
                coins.console(Level.INFO, "Added the language '" + language + "' to Coins, which can now be used in the config.");
            }
        }

        initializeMessages(Config.LANGUAGE);
    }

    public void reload() {
        if (!coins.getDisabledReasons().isEmpty()) {
            coins.line(Level.SEVERE);
            coins.console(Level.SEVERE, """
                Plugin 'Coins' is disabled, until issues are fixed and the server is \
                rebooted (see start-up log of Coins).
                """
            );
            coins.line(Level.SEVERE);
            return;
        }

        resetMultiplier();
        resetWarningCount();
        parseConfig();
        reloadLanguage();

        if (warnings != 0) {
            coins.console(Level.WARNING, """
                Loaded the config of Coins with %d warnings. Check above here for details.
                """.formatted(warnings)
            );
        }
    }

    private FileConfiguration getOrSaveConfig() {
        File config = new File(coins.getDataFolder() + File.separator + "config.yml");
        if (!config.exists()) {
            coins.saveDefaultConfig();
        }

        return YamlConfiguration.loadConfiguration(config);
    }

    public static boolean USING_LEGACY_KEYS = false; // from before version 1.12
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
                    if (configKey != null && config.contains(configKey) && !USING_LEGACY_KEYS) {
                        showWarning("You are using the old format of config keys ('" + configKey + "' instead of '" + validKey +"')." +
                                " Please update your config, as support for this will be dropped in the future.");
                        USING_LEGACY_KEYS = true;
                    }
                }

                if (configKey == null || !config.contains(configKey)) {
                    if (configEntry.required()) {
                        String prefixSuffix = field.getType() == String.class? "'" : "";
                        Object defaultValue = prefixSuffix + field.get(Config.class) + prefixSuffix;

                        showWarning(String.format(
                            "\nConfig file is missing key `%s`. Using its default value now (%s)."
                                + (configEntry.motivation().isEmpty()? "" : " " + configEntry.motivation())
                                + " Consider to add this to the config:\n----------------------------------------\n%s: %s" +
                                "\n----------------------------------------",
                            configEntry.value(), defaultValue, configEntry.value().replace(".", ":\n  "), defaultValue
                        ));
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
                    Map<String, Object> map = config.getConfigurationSection(configKey).getValues(false);
                    Map<String, Integer> configMap = new HashMap<>();

                    for (Map.Entry<String, Object> mapLoop : map.entrySet()) {
                        configMap.put(mapLoop.getKey().toUpperCase().replace(" ", "_"), Util.parseInt(mapLoop.getValue().toString()).orElse(1));
                    }
                    configValue = configMap;
                }
                else if (configClass == String.class || configClass == Material.class || configClass == Sound.class || configClass == MessagePosition.class) {
                    String value = config.getString(configKey);
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    else if (configClass == Material.class) {
                        configValue = getMaterial(value, configEntry.value()).orElse(Material.SUNFLOWER);
                    }
                    else if (configClass == Sound.class) {
                        configValue = getSound(value, configEntry.value()).orElse(Sound.ITEM_ARMOR_EQUIP_GOLD);
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
                        configValue = Util.color(value);
                    }
                }
                // todo can be improved in java 11
                else if (configClass == Long.class || configClass == Integer.class || configClass == Float.class || configClass == Double.class) {
                    Double value = Double.parseDouble(config.get(configKey, "0").toString());

                    if (configClass == Long.class) {
                        configValue = value.longValue();
                    }
                    else if (configClass == Integer.class) {
                        configValue = value.intValue();
                    }
                    else if (configClass == Float.class) {
                        configValue = value.floatValue();
                    }
                    else {
                        configValue = value;
                    }
                }
                else {
                    //configValue = config.getObject(configKey, configClass);
                    configValue = configClass.cast(config.get(configKey));
                }

                field.set(Config.class, configValue);
            }
            catch (Exception exception) {
                try {
                    Object defaultValue = field.get(Config.class);
                    showWarning(String.format(
                        "Config file has wrong value at `%s`. Using its default value now (%s).",
                        configEntry.value(),
                        defaultValue
                    ));
                }
                catch (IllegalAccessException ignored) {
                }
            }
        }

        parseRemainingOptions();
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

        Config.DECIMAL_FORMATTER = new DecimalFormat("#" + groupSeparator + "##0." + decimals, formatSymbols);
    }

    // todo also parse with Registry.MATERIAL
    private Optional<Material> getMaterial(String name, String configKey) {
        Material material = Material.matchMaterial(name.replace(" ", "_").toUpperCase().replace("COIN", "SUNFLOWER"));
        if (material == null) {
            showWarning(("The material '%s' in the config at `%s` does not exist. Please use a material " +
                "from: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html").formatted(name, configKey));
            return Optional.empty();
        }

        return Optional.of(material);
    }

    private Optional<MessagePosition> getMessagePosition(String name, String configKey) {
        try {
            return Optional.of(MessagePosition.valueOf(name.replace(" ", "_").toUpperCase()));
        }
        catch (IllegalArgumentException exception) {
            showWarning(("Message position '%s' in the config at `%s` is invalid. Use either " +
                "'actionbar', 'title', 'subtitle', or 'chat'.").formatted(name, configKey));
            return Optional.empty();
        }
    }

    private Optional<EntityType> getEntityType(String name, String configKey) {
        try {
            return Optional.of(EntityType.valueOf(name.replace(" ", "_").toUpperCase()));
        }
        catch (IllegalArgumentException exception) {
            showWarning(("The mob name '%s' in the config at `%s` does not exist. Please use a name from: " +
                "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html").formatted(name, configKey));

            return Optional.empty();
        }
    }

    // todo also parse with Registry.SOUNDS
    private Optional<Sound> getSound(String name, String configKey) {
        try {
            return Optional.of(Sound.valueOf(name.toUpperCase().replace(" ", "_")));
        }
        catch (IllegalArgumentException exception) {
            showWarning(("The sound '%s' in the config at `%s` does not exist. Please use a sound from: " +
                "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html").formatted(name, configKey));

            return Optional.empty();
        }
    }

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

    private static final Converter<String, String> VAR_CONVERTER = CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_HYPHEN);

    public TreeSet<String> getKeys() {
        TreeSet<String> values = new TreeSet<>();

        for (Field field : Config.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && !field.isAnnotationPresent(Deprecated.class)) {
                try {
                    values.add(VAR_CONVERTER.convert(field.getName()) + " &8»&7 " + Util.formatCurrency(field.get(Config.class).toString()));
                }
                catch (Exception ignored) {}
            }
        }

        return values;
    }

    public void initializeMessages(String language) {
        Optional<JSONObject> json = retrieveLanguageJson(language);
        if (json.isEmpty()) {
            coins.console(Level.WARNING, "Could not find the language file '" + language + ".json' that was configured.");
        }

        List<String> missingKeys = new ArrayList<>();
        for (Message message : Message.values()) {
            try {
                Object name = json.orElseThrow().get(message.name());
                // todo new color style for all messages in .json (no more &-codes)
                Message.MESSAGES.put(message, Util.color(Util.formatCurrency(name.toString())));
            }
            catch (Exception exception) {
                missingKeys.add(message.name());
                if (fallbackLanguage != null) {
                    Message.MESSAGES.put(message, Util.color(Util.formatCurrency(fallbackLanguage.get(message.name()).toString())));
                }
            }
        }

        if (!missingKeys.isEmpty()) {
            showWarning("Language file '" + language + "' is missing the message(s) '" + String.join("', '", missingKeys) +
                "'. Using the default value(s) now, which are in English. You can find the up-to-date default configured messages at:" +
                " https://github.com/JustEli/Coins/blob/master/src/main/resources/language/english.json");

            if (language.equalsIgnoreCase("english")) {
                coins.console(Level.WARNING, "You are using the default language (English), you could delete the English" +
                    " language file (at /Coins/language/english.json) to get rid of this warning.");
            }
        }
    }

    private Optional<JSONObject> retrieveLanguageJson(String language) {
        Optional<File> file = retrieveLanguageFile(language);
        if (file.isEmpty()) {
            return Optional.empty();
        }

        try (InputStream fileStream = Files.newInputStream(file.get().toPath())) {
            return Optional.ofNullable(jsonStream(fileStream));
        }
        catch (Exception ignored) {}

        return Optional.empty();
    }

    private JSONObject retrieveFallbackLanguage() {
        try (InputStream inputStream = coins.getResource("language/english.json")) {
            return jsonStream(inputStream);
        }
        catch (Exception exception) {
            exception.printStackTrace();
            stackTraceInfo();
        }
        return null;
    }

    private JSONObject jsonStream(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return (JSONObject) new JSONParser().parse(reader);
        }
        catch (IOException | ParseException exception) {
            exception.printStackTrace();
            stackTraceInfo();
        }
        return null;
    }

    private void stackTraceInfo() {
        coins.console(Level.WARNING, """
            The above error does not affect the plugin. Though, it is appreciated if you \
            report this error to Coins in the Discord server (https://discord.gg/fVwCETj) \
            at #coins-errors, because the error should not happen. Include this line. \
            Details[OS='%s',JAVA='%s',MC='%s']
            """.formatted(
                System.getProperty("os.name"),
                System.getProperty("java.version"),
                coins.getServer().getVersion()
            )
        );
    }

    private Optional<File> retrieveLanguageFile(String language) {
        File[] languageFiles = new File(coins.getDataFolder().getAbsolutePath() + File.separator + "language").listFiles();
        if (languageFiles == null) {
            return Optional.empty();
        }

        for (File languageFile : languageFiles) {
            if (languageFile.getName().equalsIgnoreCase(language + ".json")) {
                return Optional.of(languageFile);
            }
        }

        return Optional.empty();
    }

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
