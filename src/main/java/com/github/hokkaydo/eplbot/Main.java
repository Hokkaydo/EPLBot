package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.command.CommandManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.ModuleManager;
import com.github.hokkaydo.eplbot.module.autopin.AutoPinModule;
import com.github.hokkaydo.eplbot.module.basic.BasicCommandModule;
import com.github.hokkaydo.eplbot.module.configuration.ConfigurationModule;
import com.github.hokkaydo.eplbot.module.mirror.MirrorModule;
import com.github.hokkaydo.eplbot.module.quote.QuoteModule;
import com.github.hokkaydo.eplbot.module.rss.RssModule;
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

        registerModules(moduleManager);
        jda.getGuilds().forEach(guild ->
                                        moduleManager.enableModules(
                                                guild.getIdLong(),
                                                Config.getGuildModulesStatus(
                                                                guild.getIdLong(),
                                                                moduleManager.getModuleNames())
                                                        .entrySet()
                                                        .stream()
                                                        .filter(Map.Entry::getValue)
                                                        .map(Map.Entry::getKey)
                                                        .toList()
                                        )
        );
    }

    private static void registerModules(ModuleManager moduleManager) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<Class<? extends Module>> modules = Arrays.asList(AutoPinModule.class, ConfigurationModule.class, MirrorModule.class, QuoteModule.class, RssModule.class, BasicCommandModule.class);

        for (Class<? extends Module> moduleClazz : modules) {
            for (Guild guild : jda.getGuilds()) {
                Module module = moduleClazz.getDeclaredConstructor(Long.class).newInstance(guild.getIdLong());
                moduleManager.addModule(module);
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
