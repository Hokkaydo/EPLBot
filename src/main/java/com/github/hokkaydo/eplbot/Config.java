package com.github.hokkaydo.eplbot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Config {

    private static final String CONFIG_PATH = "./config";

    private static final Map<String, ConfigurationParser> DEFAULT_CONFIGURATION = Map.of(
            "PIN_REACTION_NAME", new ConfigurationParser(
                    "\uD83D\uDCCC",
                    Object::toString,
                    o -> o,
                    "Nom de la réaction"
            ),
            "PIN_REACTION_THRESHOLD", new ConfigurationParser(
                    1,
                    Object::toString,
                    Integer::parseInt,
                    "Nombre entier"
            ),
            "configuration", new ConfigurationParser(true, Object::toString, Boolean::valueOf, "Booléen"),
            "autopin", new ConfigurationParser(false, Object::toString, Boolean::valueOf, "Booléen"),
            "mirror", new ConfigurationParser(false, Object::toString, Boolean::valueOf, "Booléen")
    );
    private static final Map<String, Object> GLOBAL_CONFIGURATION = new HashMap<>();
    private static final Map<Long, Map<String, Object>> GUILD_CONFIGURATION = new HashMap<>();


    public static <T> T getGlobalValue(String key) {
        return (T)GLOBAL_CONFIGURATION.getOrDefault(key, DEFAULT_CONFIGURATION.get(key).defaultValue);
    }

    public static <T> T getGuildValue(Long guildId, String key) {
        return (T)GUILD_CONFIGURATION.getOrDefault(guildId, new HashMap<>()).getOrDefault(key, DEFAULT_CONFIGURATION.get(key).defaultValue);
    }

    public static String getValueFormat(String key) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) return "KEY_ERROR";
        return DEFAULT_CONFIGURATION.get(key).format;
    }

    public static void updateGlobalValue(String key, Object value) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) return;
        GLOBAL_CONFIGURATION.put(key, value);
        saveValue(0L, key, value);
    }

    public static void updateGuildValue(Long guildId, String key, Object value) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) return;
        if(!GUILD_CONFIGURATION.containsKey(guildId))
            GUILD_CONFIGURATION.put(guildId, new HashMap<>());
        GUILD_CONFIGURATION.get(guildId).put(key, value);
        saveValue(guildId, key, value);
    }

    public static void disableGuildModule(Long guildId, String key) {
        updateGuildValue(guildId, key, false);
    }

    public static void enableGuildModule(Long guildId, String key) {
        updateGuildValue(guildId, key, true);
    }

    public static void disableGlobalModule(String key) {
        updateGlobalValue(key, false);
    }

    public static void enableGlobalModule(String key) {
        updateGlobalValue(key, true);
    }

    public static boolean getGlobalModuleStatus(String moduleName) {
        return (boolean)GLOBAL_CONFIGURATION.getOrDefault(moduleName, Optional.ofNullable(DEFAULT_CONFIGURATION.get(moduleName)).map(ConfigurationParser::defaultValue).orElse(false));
    }

    public static boolean getGuildModuleStatus(Long guildId, String moduleName) {
        return (boolean)GUILD_CONFIGURATION.getOrDefault(guildId, new HashMap<>()).getOrDefault(moduleName, Optional.ofNullable(DEFAULT_CONFIGURATION.get(moduleName)).map(ConfigurationParser::defaultValue).orElse(false));
    }

    public static Map<String, Boolean> getGlobalModulesStatus(List<String> moduleNames) {
        return moduleNames.stream()
                       .map(name -> Map.entry(name, getGlobalModuleStatus(name)))
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    public static Map<String, Boolean> getGuildModulesStatus(Long guildId, List<String> moduleNames) {
        return moduleNames.stream()
                       .map(name -> Map.entry(name, getGuildModuleStatus(guildId, name)))
                       .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static void saveValue(Long guildId, String key, Object value) {
        if(!DEFAULT_CONFIGURATION.containsKey(key)) throw new IllegalStateException("Configuration key not allowed");
        Properties prop = new Properties();
        try(FileInputStream input = new FileInputStream(CONFIG_PATH)){
            prop.load(input);
        } catch(IOException ignored) {
            throw new IllegalStateException("Could not load config file");
        }
        String k = (guildId == 0 ? "%" : "" ) + guildId + ";" + key;
        prop.setProperty(k, DEFAULT_CONFIGURATION.get(key).toConfig.apply(value));
        try(FileOutputStream output = new FileOutputStream(CONFIG_PATH)){
            prop.store(output, "");
        } catch(IOException e) {
            throw new IllegalStateException("Could not save config file");
        }
    }

    protected static void load() {
        Properties prop = new Properties();
        try(FileInputStream stream = new FileInputStream(CONFIG_PATH)) {
            prop.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        prop.forEach((a, b) -> {
            String v = b.toString();
            String[] keySplit = a.toString().split(";");
            String key = keySplit[1];
            Long guildId = Long.parseLong(keySplit[0]);
            if(!DEFAULT_CONFIGURATION.containsKey(key)) return;
            if(guildId == 0) {
                GLOBAL_CONFIGURATION.put(key, v);
            }else {
                if(!GUILD_CONFIGURATION.containsKey(guildId)) {
                    GUILD_CONFIGURATION.put(guildId, new HashMap<>(DEFAULT_CONFIGURATION.entrySet().stream().map(e -> Map.entry(e.getKey(),e.getValue().defaultValue)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
                }
                GUILD_CONFIGURATION.get(guildId).put(key, DEFAULT_CONFIGURATION.get(key).fromConfig.apply(v));
            }
        });
    }


    private record ConfigurationParser(Object defaultValue,
                                       Function<Object, String> toConfig,
                                       Function<String, Object> fromConfig,
                                       String format) {}

}
