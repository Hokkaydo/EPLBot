package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.module.GlobalModule;
import com.github.hokkaydo.eplbot.module.GuildModule;
import com.github.hokkaydo.eplbot.module.ModuleManager;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {

    private static JDA jda;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final String token = Dotenv.load().get("DISCORD_BOT_TOKEN");
        final ModuleManager moduleManager = new ModuleManager();
        registerModules(moduleManager);
        Config.load();
        Strings.load();
        jda = JDABuilder.createDefault(token)
                      .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                      .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                      .setBulkDeleteSplittingEnabled(false)
                      .setActivity(Activity.watching("you"))
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
        List<Class<GlobalModule>> globalModules = Arrays.asList();
        List<Class<GuildModule>> guildModules = Arrays.asList();

        for (Class<GlobalModule> globalModuleClazz : globalModules) {
            GlobalModule module = globalModuleClazz.getDeclaredConstructor().newInstance();
            moduleManager.addGlobalModule(module);
        }

        for (Class<GuildModule> guildModuleClazz : guildModules) {
            for (Guild guild : jda.getGuilds()) {
                GuildModule module = guildModuleClazz.getDeclaredConstructor(Guild.class).newInstance(guild);
                moduleManager.addGuildModule(module);
            }
        }
    }

    public static JDA getJDA() {
        return jda;
    }
}
