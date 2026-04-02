package com.github.hokkaydo.eplbot.configuration;

import com.github.hokkaydo.eplbot.configuration.model.ConfigurationModel;
import com.github.hokkaydo.eplbot.configuration.repository.ConfigurationRepository;
import com.github.hokkaydo.eplbot.configuration.repository.ConfigurationRepositorySQLite;
import com.github.hokkaydo.eplbot.database.DatabaseManager;

import java.awt.Color;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents the configuration of the bot.
 * It mainly contains 2 types of configuration:
 * <ul>
 *     <li>Configuration: Configuration entries that can be modified by the user</li>
 *     <li>State: Configuration entries that are used to store the state of the bot on a guild</li>
 * </ul>
 * This distinction has been made to be able to store small information about the state of the bot in a guild
 * without having to create a dedicated table for a simple string.
 * Typically used for storing the last Early/Night bird message.
 * <br>
 *
 * The class contains 2 final {@link Map}: {@link #DEFAULT_CONFIGURATION}
 * and {@link #DEFAULT_STATE} associating a key to a {@link ConfigurationParser}.
 * Those {@link ConfigurationParser} are used
 * to parse the configuration value from a {@link String} to the desired type and vice versa.
 *
 * <br>
 * Aside those {@link Map}s, the class also registers the current configuration and state of a guild
 * */
public class Config {

    private static final String STRING_FORMAT = "Chaîne de caractères";
    private static final Supplier<ConfigurationParser> MODULE_DISABLED = () -> new ConfigurationParser(() -> false, Object::toString, Boolean::valueOf, "Booléen");
    private static final String INTEGER_FORMAT = "Nombre entier";
    private static final String COLOR_FORMAT = "RGB sous forme hexadécimale : Ex #FFFFFF = Blanc";

    private static final IntFunction<ConfigurationParser> INTEGER_CONFIGURATION_VALUE = init -> new ConfigurationParser(
            () -> init,
            Object::toString,
            Integer::parseInt,
            INTEGER_FORMAT
    );
    private static final LongFunction<ConfigurationParser> LONG_CONFIGURATION_VALUE = init -> new ConfigurationParser(
            () -> init,
            Object::toString,
            Long::parseLong,
            INTEGER_FORMAT
    );
    private static final Supplier<ConfigurationParser> STRING_CONFIGURATION_VALUE = () -> new ConfigurationParser(
            () -> "",
            Object::toString,
            s -> s,
            STRING_FORMAT
    );
    private static final Function<String, ConfigurationParser> STRING_CONFIGURATION_VALUE_DEFAULT = str -> new ConfigurationParser(
            () -> str,
            Object::toString,
            s -> s,
            STRING_FORMAT
    );

    private static final Function<Color, ConfigurationParser> COLOR_CONFIGURATION_VALUE = init -> new ConfigurationParser(
            () -> init,
            c -> "#%s".formatted(Integer.toHexString(((Color) c).getRGB()).substring(2)),
            Color::decode,
            COLOR_FORMAT
    );

    private static ConfigurationRepository repository;
    private static final Map<String, ConfigurationParser> DEFAULT_CONFIGURATION = new HashMap<>(Map.of(
            "PIN_REACTION_NAME", new ConfigurationParser(
                    () -> "\uD83D\uDCCC",
                    Object::toString,
                    o -> o,
                    "Nom de la réaction"
            ),
            "PIN_REACTION_THRESHOLD", INTEGER_CONFIGURATION_VALUE.apply(1),
            "ADMIN_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
            "CONFESSION_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
            "CONFESSION_VALIDATION_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
            "CONFESSION_EMBED_COLOR", COLOR_CONFIGURATION_VALUE.apply(Color.decode("#3498DB")),
            "DRIVE_ADMIN_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
            "COMMAND_CODE_TIMELIMIT", INTEGER_CONFIGURATION_VALUE.apply(30)
    ));

    public static Map<String, ConfigurationParser> getDefaultConfiguration() {
        return Collections.unmodifiableMap(DEFAULT_CONFIGURATION);
    }

    private static final Map<String, ConfigurationParser> DEFAULT_STATE = Map.of(
            "LAST_RSS_ARTICLE_DATE", new ConfigurationParser(
                    () -> new HashMap<>(Map.of("https://www.developpez.com/index/rss", Timestamp.from(Instant.EPOCH))),
                    m -> ((Map<String, Timestamp>) m).entrySet().stream().map(e -> "%s;%s".formatted(e.getKey(), e.getValue())).reduce("", (a, b) ->  a.isBlank() ? b : "%s,%s".formatted(a, b)),
                    s -> Arrays.stream(s.split(",")).map(a -> a.split(";")).map(a -> Map.entry(a[0], Timestamp.valueOf(a[1]))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    "Liste de paires Lien-Timestamp"
            ),
            "EXAM_RETRIEVE_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
            "EXAM_ZIP_MESSAGE_ID", STRING_CONFIGURATION_VALUE.get(),
            "EARLY_BIRD_NEXT_MESSAGE", STRING_CONFIGURATION_VALUE.get(),
            "NIGHT_BIRD_NEXT_MESSAGE", STRING_CONFIGURATION_VALUE.get()
    );
    static {
        // Configuration keys
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "RSS_FEEDS", new ConfigurationParser(
                        () -> new ArrayList<>(List.of("https://www.developpez.com/index/rss")),
                        l -> ((List<String>)l).stream().reduce("", "%s;%s"::formatted),
                        s -> Stream.of(s.split(";")).filter(str -> !str.isBlank()).toList(),
                        "Liste de liens séparés par `;`"
                ),
                "RSS_FEEDS_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
                "RSS_FEEDS_COLOR", COLOR_CONFIGURATION_VALUE.apply(Color.YELLOW),
                "RSS_UPDATE_PERIOD", LONG_CONFIGURATION_VALUE.apply(15L),
                "ADMIN_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
                "CONFESSION_WARN_THRESHOLD", INTEGER_CONFIGURATION_VALUE.apply(3),
                "EARLY_BIRD_ROLE_ID", STRING_CONFIGURATION_VALUE.get(),
                "EARLY_BIRD_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
                "EARLY_BIRD_RANGE_START_DAY_SECONDS", LONG_CONFIGURATION_VALUE.apply(6*60*60L),
                "EARLY_BIRD_RANGE_END_DAY_SECONDS", LONG_CONFIGURATION_VALUE.apply(9*60*60L)
        ));
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "NIGHT_BIRD_ROLE_ID", STRING_CONFIGURATION_VALUE.get(),
                "NIGHT_BIRD_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
                "NIGHT_BIRD_RANGE_START_DAY_SECONDS", LONG_CONFIGURATION_VALUE.apply(2*60*60L),
                "NIGHT_BIRD_RANGE_END_DAY_SECONDS", LONG_CONFIGURATION_VALUE.apply(5*60*60L),
                "EARLY_BIRD_MESSAGE_PROBABILITY", INTEGER_CONFIGURATION_VALUE.apply(33),
                "NIGHT_BIRD_MESSAGE_PROBABILITY", INTEGER_CONFIGURATION_VALUE.apply(33),
                "ASSISTANT_ROLE_ID", STRING_CONFIGURATION_VALUE.get(),
                "MODERATOR_ROLE_ID", STRING_CONFIGURATION_VALUE.get(),
                "HELPER_CATEGORY_IDS", new ConfigurationParser(
                        List::of,
                        l -> ((List<String>)l).stream().reduce("", "%s;%s"::formatted),
                        s -> Stream.of(s.split(";")).filter(str -> !str.isBlank()).toList(),
                        "Liste d'identifiants de catégories séparés par `;`"
                ),
                "MENU_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get()
        ));

        DEFAULT_CONFIGURATION.putAll(Map.of(
                "NIGHT_BIRD_UNICODE_REACT_EMOJI", STRING_CONFIGURATION_VALUE_DEFAULT.apply("🌙"),
                "EARLY_BIRD_UNICODE_REACT_EMOJI", STRING_CONFIGURATION_VALUE_DEFAULT.apply("❤"),
                "TRANSLATION_LANGUAGE", STRING_CONFIGURATION_VALUE_DEFAULT.apply("nl")
        ));

        // Modules
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "configuration", new ConfigurationParser(() -> true, Object::toString, Boolean::valueOf, "Booléen"),
                "autopin", MODULE_DISABLED.get(),
                "rss", MODULE_DISABLED.get(),
                "mirror", MODULE_DISABLED.get(),
                "confession", MODULE_DISABLED.get(),
                "eplmodule", MODULE_DISABLED.get(),
                "quote", MODULE_DISABLED.get(),
                "examsretrieve", MODULE_DISABLED.get(),
                "ratio", MODULE_DISABLED.get(),
                "notice", MODULE_DISABLED.get()
        ));
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "messagebird", MODULE_DISABLED.get(),
                "christmas", MODULE_DISABLED.get(),
                "bookmark", MODULE_DISABLED.get(),
                "code", MODULE_DISABLED.get(),
                "helper", MODULE_DISABLED.get(),
                "menu", MODULE_DISABLED.get(),
                "data", MODULE_DISABLED.get(),
                "translation", MODULE_DISABLED.get()
        ));
    }
    private static final Map<Long, Map<String, Object>> GUILD_CONFIGURATION = new HashMap<>();
    private static final Map<Long, Map<String, Object>> GUILD_STATE = new HashMap<>();

    private static final Supplier<Map<String, Object>> DEFAULT_CONFIGURATION_VALUES = () -> new HashMap<>(DEFAULT_CONFIGURATION.entrySet().stream().map(e -> Map.entry(e.getKey(),e.getValue().defaultValue.get())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    private static final Supplier<Map<String, Object>> DEFAULT_STATE_VALUES = () -> new HashMap<>(DEFAULT_STATE.entrySet().stream().map(e -> Map.entry(e.getKey(),e.getValue().defaultValue.get())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    /**
     * Retrieves the value of a configuration entry for a guild.
     * @param guildId the id of the guild
     * @param key the key of the configuration entry
     * @param <T> the type of the configuration entry
     * @return the value of the configuration entry if found,
     * default value if the guild has no entry for this key, null if the key is not allowed
     * */
    public static <T> T getGuildVariable(Long guildId, String key) {
        return getGuildValue(guildId, key, GUILD_CONFIGURATION, DEFAULT_CONFIGURATION);
    }

    /**
     * Retrieves the value of a configuration entry for a guild.
     * @param guildId the id of the guild
     * @param key the key of the configuration entry
     * @param <T> the type of the configuration entry
     * @param map the map containing the configuration entries
     *            (either {@link #GUILD_CONFIGURATION} or {@link #GUILD_STATE})
     * @param defaultMap the map containing the default configuration entries
     *                   (either {@link #DEFAULT_CONFIGURATION} or {@link #DEFAULT_STATE})
     * @return the value of the configuration entry if found,
     * default value if the guild has no entry for this key, null if the key is not allowed
     * */
    @SuppressWarnings("unchecked")
    private static <T> T getGuildValue(Long guildId, String key, Map<Long, Map<String, Object>> map, Map<String, ConfigurationParser> defaultMap) {
        return (T)map.getOrDefault(guildId, new HashMap<>())
                          .getOrDefault(
                                  key,
                                  Optional.ofNullable(defaultMap.get(key))
                                          .map(ConfigurationParser::defaultValue)
                                          .map(Supplier::get)
                                          .orElse(null)
                          );
    }

    /**
     * Retrieves the value of a state entry for a guild.
     * @param guildId the id of the guild
     * @param key the key of the state entry
     * @param <T> the type of the state entry
     * @return the value of the state entry if found,
     * default value if the guild has no entry for this key, null if the key is not allowed
     * */
    public static <T> T getGuildState(Long guildId, String key) {
        return getGuildValue(guildId, key, GUILD_STATE, DEFAULT_STATE);
    }

    /**
     * Retrieves the value format of a configuration entry.
     * @param key the key of the configuration entry
     * @return the format of the configuration entry
     * @throws IllegalStateException if the key is not allowed
     * */
    public static String getValueFormat(String key) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) throw new IllegalStateException("KEY_ERROR");
        return DEFAULT_CONFIGURATION.get(key).format;
    }

    /**
     * Parses a configuration entry through its {@link ConfigurationParser} and updates its value.
     * @param guildId the id of the guild
     * @param key the key of the configuration entry
     * @param value the value of the configuration entry
     * @return true if the key is allowed, false otherwise
     * */
    public static boolean parseAndUpdate(Long guildId, String key, String value) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) return false;
        updateValue(guildId, key, DEFAULT_CONFIGURATION.get(key).fromConfig.apply(value));
        return true;
    }

    /**
     * Updates the value of a configuration entry.
     * @param guildId the id of the guild
     * @param key the key of the configuration entry
     * @param value the value of the configuration entry
     * @throws IllegalStateException if the key is not allowed
     * */
    public static void updateValue(Long guildId, String key, Object value) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) {
            if(!DEFAULT_STATE.containsKey(key)) throw new IllegalStateException("Configuration key isn't allowed");
            GUILD_STATE.computeIfAbsent(guildId, ignored -> new HashMap<>());
            GUILD_STATE.get(guildId).put(key, value);
            saveValue(guildId, key, value);
        }
        GUILD_CONFIGURATION.computeIfAbsent(guildId, ignored -> new HashMap<>());
        GUILD_CONFIGURATION.get(guildId).put(key, value);
        saveValue(guildId, key, value);
    }

    /**
     * Disables a module for a guild in the configuration.
     * @param guildId the id of the guild
     * @param key the key of the module
     * */
    public static void disableModule(Long guildId, String key) {
        updateValue(guildId, key, false);
    }

    /**
     * Enables a module for a guild in the configuration.
     * @param guildId the id of the guild
     * @param key the key of the module
     * */
    public static void enableModule(Long guildId, String key) {
        updateValue(guildId, key, true);
    }

    /**
     * Retrieves the statuses of multiple modules for a guild.
     * @param guildId the id of the guild
     * @param moduleNames the names of the modules
     * @return a {@link Map} containing the status of each module
     * @see #getModuleStatus(Long, String)
     * */
    public static Map<String, Boolean> getModulesStatuses(Long guildId, List<String> moduleNames) {
        return moduleNames.stream()
                       .map(name -> Map.entry(name, getModuleStatus(guildId, name)))
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Retrieves the status of a module for a guild.
     * @param guildId the id of the guild
     * @param moduleName the name of the module
     * @return true if enabled, false if disabled
     * */
    public static boolean getModuleStatus(Long guildId, String moduleName) {
        return (boolean)GUILD_CONFIGURATION.getOrDefault(guildId, new HashMap<>()).getOrDefault(moduleName, Optional.ofNullable(DEFAULT_CONFIGURATION.get(moduleName)).map(ConfigurationParser::defaultValue).map(Supplier::get).orElse(false));
    }

    private static void saveValue(Long guildId, String key, Object value) {
        if(DEFAULT_CONFIGURATION.containsKey(key)) {
            GUILD_CONFIGURATION.computeIfAbsent(guildId, ignored -> DEFAULT_CONFIGURATION_VALUES.get());
            String val = DEFAULT_CONFIGURATION.get(key).toConfig.apply(value);
            repository.updateGuildVariable(guildId, key, val);
            GUILD_CONFIGURATION.get(guildId).put(key, value);
        } else {
            if(!DEFAULT_STATE.containsKey(key)) return;
            GUILD_STATE.computeIfAbsent(guildId, ignored -> DEFAULT_STATE_VALUES.get());
            String val = DEFAULT_STATE.get(key).toConfig.apply(value);
            repository.updateGuildState(guildId, key, val);
            GUILD_STATE.get(guildId).put(key, value);
        }
    }

    /**
     * Loads the configuration from the database.
     * */
    public static void load() {
        repository = new ConfigurationRepositorySQLite(DatabaseManager.getDataSource());

        repository.getGuildStates()
                .stream()
                .filter(m -> DEFAULT_STATE.containsKey(m.key()))
                .forEach(m -> load(DEFAULT_STATE, GUILD_STATE, m));
        repository.getGuildVariables()
                .stream()
                .filter(m -> DEFAULT_CONFIGURATION.containsKey(m.key()))
                .forEach(m -> load(DEFAULT_CONFIGURATION, GUILD_CONFIGURATION, m));
    }

    /**
     * Loads a configuration entry from the database.
     * @param defaultValues the default values of the configuration entries
     * @param current the current configuration entries
     * @param m the {@link ConfigurationModel} to load a value from
     * */
    private static void load(Map<String, Config.ConfigurationParser> defaultValues, Map<Long, Map<String, Object>> current, ConfigurationModel m) {
        if(!current.containsKey(m.guildId()))
            current.put(m.guildId(), new HashMap<>(defaultValues.entrySet().stream().map(e -> Map.entry(e.getKey(),e.getValue().defaultValue.get())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
        current.get(m.guildId()).put(m.key(), defaultValues.get(m.key()).fromConfig.apply(m.value()));
    }

    /**
     * @return the default state of the configuration
     * */
    public static Map<String, ConfigurationParser> getDefaultState() {
        return DEFAULT_STATE;
    }

    /**
     * Resets the default state of the configuration for a guild.
     * @param guildId the id of the guild
     * */
    public static void resetDefaultState(Long guildId) {
        for (Map.Entry<String, ConfigurationParser> entry : DEFAULT_STATE.entrySet()) {
            updateValue(guildId, entry.getKey(), entry.getValue().defaultValue.get());
        }
    }

    /**
     * Resets the default configuration of the configuration for a guild.
     * @param defaultValue the default value of the configuration entry
     * @param toConfig the {@link Function} to convert the value to a {@link String}
     * @param fromConfig the {@link Function} to convert the value from a {@link String}
     * @param format the format of the configuration entry, explains the expected value's format
     * */
    public record ConfigurationParser(Supplier<Object> defaultValue,
                                      Function<Object, String> toConfig,
                                      Function<String, Object> fromConfig,
                                      String format) {}

}
