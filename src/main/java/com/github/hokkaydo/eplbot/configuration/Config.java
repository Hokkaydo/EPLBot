package com.github.hokkaydo.eplbot.configuration;

import com.github.hokkaydo.eplbot.configuration.model.ConfigurationModel;
import com.github.hokkaydo.eplbot.configuration.repository.ConfigurationRepository;
import com.github.hokkaydo.eplbot.configuration.repository.ConfigurationRepositorySQLite;
import com.github.hokkaydo.eplbot.database.DatabaseManager;

import java.awt.*;
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

public class Config {

    private static final String STRING_FORMAT = "Chaîne de caractères";
    private static final Supplier<ConfigurationParser> MODULE_DISABLED = () -> new ConfigurationParser(() -> false, Object::toString, Boolean::valueOf, "Booléen");
    private static final String INTEGER_FORMAT = "Nombre entier";
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
            "CONFESSION_EMBED_COLOR", new ConfigurationParser(
                    () -> Color.decode("#3498DB"),
                    c -> Integer.toString(((Color)c).getRGB(), 16),
                    Color::decode,
                    "RGB sous forme hexadécimale : Ex #FFFFFF = Blanc"
            ),
            "DRIVE_ADMIN_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get()
    ));

    public static Map<String, ConfigurationParser> getDefaultConfiguration() {
        return Collections.unmodifiableMap(DEFAULT_CONFIGURATION);
    }

    private static final Map<String, ConfigurationParser> DEFAULT_STATE = Map.of(
            "LAST_RSS_ARTICLE_DATE", new ConfigurationParser(
                    () -> new HashMap<>(Map.of("https://www.developpez.com/index/rss", Timestamp.from(Instant.EPOCH))),
                    m -> ((Map<String, Timestamp>) m).entrySet().stream().map(e -> STR."\{e.getKey()};\{e.getValue()}").reduce("", (a, b) ->  a.isBlank() ? b : STR."\{a},\{b}"),
                    s -> Arrays.stream(s.split(",")).map(a -> a.split(";")).map(a -> Map.entry(a[0], Timestamp.valueOf(a[1]))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    "Liste de paires Lien-Timestamp"
            ),
            "EXAM_RETRIEVE_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
            "EXAM_ZIP_MESSAGE_ID", STRING_CONFIGURATION_VALUE.get(),
            "EARLY_BIRD_NEXT_MESSAGE", STRING_CONFIGURATION_VALUE.get()
    );
    static {
        // Configuration keys
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "RSS_FEEDS", new ConfigurationParser(
                        () -> new ArrayList<>(List.of("https://www.developpez.com/index/rss")),
                        Object::toString,
                        s -> List.of(s.split(";")),
                        "Liste de liens séparés par `;`"
                ),
                "RSS_FEEDS_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
                "RSS_FEEDS_COLOR", new ConfigurationParser(
                        () -> Color.YELLOW,
                        c -> Integer.toString(((Color)c).getRGB(), 16),
                        Color::decode,
                        "RGB sous forme hexadécimale : Ex #FFFFFF = Blanc"
                ),
                "RSS_UPDATE_PERIOD", LONG_CONFIGURATION_VALUE.apply(15L),
                "ADMIN_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
                "CONFESSION_WARN_THRESHOLD", INTEGER_CONFIGURATION_VALUE.apply(3),
                "EARLY_BIRD_ROLE_ID", STRING_CONFIGURATION_VALUE.get(),
                "EARLY_BIRD_CHANNEL_ID", STRING_CONFIGURATION_VALUE.get(),
                "EARLY_BIRD_RANGE_START_DAY_SECONDS", LONG_CONFIGURATION_VALUE.apply(6*60*60L),
                "EARLY_BIRD_RANGE_END_DAY_SECONDS", LONG_CONFIGURATION_VALUE.apply(9*60*60L)
        ));
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "EARLY_BIRD_MESSAGE_PROBABILITY", INTEGER_CONFIGURATION_VALUE.apply(33),
                "ASSISTANT_ROLE_ID", STRING_CONFIGURATION_VALUE.get(),
                "MODERATOR_ROLE_ID", STRING_CONFIGURATION_VALUE.get()
        ));

        // Modules
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "configuration", new ConfigurationParser(() -> true, Object::toString, Boolean::valueOf, "Booléen"),
                "autopin", MODULE_DISABLED.get(),
                "rss", MODULE_DISABLED.get(),
                "mirror", MODULE_DISABLED.get(),
                "confession", MODULE_DISABLED.get(),
                "basiccommands", MODULE_DISABLED.get(),
                "quote", MODULE_DISABLED.get(),
                "examsretrieve", MODULE_DISABLED.get(),
                "ratio", MODULE_DISABLED.get(),
                "notice", MODULE_DISABLED.get()

        ));
        DEFAULT_CONFIGURATION.putAll(Map.of(
                "earlybird", MODULE_DISABLED.get(),
                "christmas", MODULE_DISABLED.get(),
                "bookmark", MODULE_DISABLED.get(),
                "points", MODULE_DISABLED.get(),
                "shop", MODULE_DISABLED.get()
        ));
    }
    private static final Map<Long, Map<String, Object>> GUILD_CONFIGURATION = new HashMap<>();
    private static final Map<Long, Map<String, Object>> GUILD_STATE = new HashMap<>();

    private static final Supplier<Map<String, Object>> DEFAULT_CONFIGURATION_VALUES = () -> new HashMap<>(DEFAULT_CONFIGURATION.entrySet().stream().map(e -> Map.entry(e.getKey(),e.getValue().defaultValue.get())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    private static final Supplier<Map<String, Object>> DEFAULT_STATE_VALUES = () -> new HashMap<>(DEFAULT_STATE.entrySet().stream().map(e -> Map.entry(e.getKey(),e.getValue().defaultValue.get())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));


    public static <T> T getGuildVariable(Long guildId, String key) {
        return getGuildValue(guildId, key, GUILD_CONFIGURATION, DEFAULT_CONFIGURATION);
    }

    public static <T> T getGuildState(Long guildId, String key) {
        return getGuildValue(guildId, key, GUILD_STATE, DEFAULT_STATE);
    }

    private static <T> T getGuildValue(Long guildId, String key, Map<Long, Map<String, Object>> map, Map<String, ConfigurationParser> defaultMap) {
        return (T)map.getOrDefault(guildId, new HashMap<>()).getOrDefault(key, defaultMap.get(key).defaultValue.get());
    }

    public static String getValueFormat(String key) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) return "KEY_ERROR";
        return DEFAULT_CONFIGURATION.get(key).format;
    }

    public static boolean parseAndUpdate(Long guildId, String key, String value) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) return false;
        updateValue(guildId, key, DEFAULT_CONFIGURATION.get(key).fromConfig.apply(value));
        return true;
    }

    public static void updateValue(Long guildId, String key, Object value) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) {
            if(!DEFAULT_STATE.containsKey(key)) throw new IllegalStateException("Configuration key isn't allowed");
            GUILD_STATE.computeIfAbsent(guildId, _ -> new HashMap<>());
            GUILD_STATE.get(guildId).put(key, value);
            saveValue(guildId, key, value);
        }
        GUILD_CONFIGURATION.computeIfAbsent(guildId, _ -> new HashMap<>());
        GUILD_CONFIGURATION.get(guildId).put(key, value);
        saveValue(guildId, key, value);
    }

    public static void disableModule(Long guildId, String key) {
        updateValue(guildId, key, false);
    }

    public static void enableModule(Long guildId, String key) {
        updateValue(guildId, key, true);
    }

    public static boolean getModuleStatus(Long guildId, String moduleName) {
        return (boolean)GUILD_CONFIGURATION.getOrDefault(guildId, new HashMap<>()).getOrDefault(moduleName, Optional.ofNullable(DEFAULT_CONFIGURATION.get(moduleName)).map(ConfigurationParser::defaultValue).map(Supplier::get).orElse(false));
    }

    public static Map<String, Boolean> getModulesStatuses(Long guildId, List<String> moduleNames) {
        return moduleNames.stream()
                       .map(name -> Map.entry(name, getModuleStatus(guildId, name)))
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static void saveValue(Long guildId, String key, Object value) {
        if(DEFAULT_CONFIGURATION.containsKey(key)) {
            GUILD_CONFIGURATION.computeIfAbsent(guildId, _ -> DEFAULT_CONFIGURATION_VALUES.get());
            String val = DEFAULT_CONFIGURATION.get(key).toConfig.apply(value);
            repository.updateGuildVariable(guildId, key, val);
            GUILD_CONFIGURATION.get(guildId).put(key, value);
        } else {
            if(!DEFAULT_STATE.containsKey(key)) return;
            GUILD_STATE.computeIfAbsent(guildId, _ -> DEFAULT_STATE_VALUES.get());
            String val = DEFAULT_STATE.get(key).toConfig.apply(value);
            repository.updateGuildState(guildId, key, val);
            GUILD_STATE.get(guildId).put(key, value);
        }
    }

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

    private static void load(Map<String, Config.ConfigurationParser> defaultValues, Map<Long, Map<String, Object>> current, ConfigurationModel m) {
        if(!current.containsKey(m.guildId()))
            current.put(m.guildId(), new HashMap<>(defaultValues.entrySet().stream().map(e -> Map.entry(e.getKey(),e.getValue().defaultValue.get())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
        current.get(m.guildId()).put(m.key(), defaultValues.get(m.key()).fromConfig.apply(m.value()));

    }

    public static Map<String, ConfigurationParser> getDefaultState() {
        return DEFAULT_STATE;
    }

    public static void resetDefaultState(Long guildId) {
        for (Map.Entry<String, ConfigurationParser> entry : DEFAULT_STATE.entrySet()) {
            updateValue(guildId, entry.getKey(), entry.getValue().defaultValue.get());
        }
    }


    public record ConfigurationParser(Supplier<Object> defaultValue,
                                      Function<Object, String> toConfig,
                                      Function<String, Object> fromConfig,
                                      String format) {}

}
