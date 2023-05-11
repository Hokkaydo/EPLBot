package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.command.CommandManager;
import com.github.hokkaydo.eplbot.module.GlobalModule;
import com.github.hokkaydo.eplbot.module.GuildModule;
import com.github.hokkaydo.eplbot.module.ModuleManager;
import com.github.hokkaydo.eplbot.module.autopin.AutoPinModule;
import com.github.hokkaydo.eplbot.module.configuration.ConfigurationModule;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Main {

    private static JDA jda;
    private static ModuleManager moduleManager;
    private static CommandManager commandManager;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final String token = Dotenv.load().get("DISCORD_BOT_TOKEN");
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        Config.load();
        Strings.load();
        jda = JDABuilder.createDefault(token)
                      .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                      .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                      .setBulkDeleteSplittingEnabled(false)
                      .setActivity(Activity.watching("you"))
                      .addEventListeners(commandManager)
                      .build();
        jda.awaitReady();

        moduleManager.enableGlobalModules(
                Config.getGlobalModulesStatus(moduleManager.getGlobalModuleNames())
                        .entrySet()
                        .stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .toList()
        );
        registerModules(moduleManager);
        jda.getGuilds().forEach(guild ->
                                        moduleManager.enableGuildModules(
                                                guild.getIdLong(),
                                                Config.getGuildModulesStatus(
                                                                guild.getIdLong(),
                                                                moduleManager.getGuildModuleNames())
                                                        .entrySet()
                                                        .stream()
                                                        .filter(Map.Entry::getValue)
                                                        .map(Map.Entry::getKey)
                                                        .toList()
                                        )
        );
    }

    private static void registerModules(ModuleManager moduleManager) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Class<? extends GlobalModule>> globalModules = Collections.emptyList();
        List<Class<? extends GuildModule>> guildModules = Arrays.asList(AutoPinModule.class, ConfigurationModule.class);

        for (Class<? extends GlobalModule> globalModuleClazz : globalModules) {
            GlobalModule module = globalModuleClazz.getDeclaredConstructor().newInstance();
            moduleManager.addGlobalModule(module);
        }

        for (Class<? extends GuildModule> guildModuleClazz : guildModules) {
            for (Guild guild : jda.getGuilds()) {
                GuildModule module = guildModuleClazz.getDeclaredConstructor(Long.class).newInstance(guild.getIdLong());
                moduleManager.addGuildModule(module);
            }
        }
    }

    public static JDA getJDA() {
        return jda;
    }
    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static CommandManager getCommandManager() {
        return commandManager;
    }
}
