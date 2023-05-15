package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.ModuleManager;
import com.github.hokkaydo.eplbot.module.autopin.AutoPinModule;
import com.github.hokkaydo.eplbot.module.basic.BasicCommandModule;
import com.github.hokkaydo.eplbot.module.confession.ConfessionModule;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static JDA jda;
    private static ModuleManager moduleManager;
    private static CommandManager commandManager;

    private static final List<Activity> status = List.of(
            Activity.playing("bâtir des ponts (solides) entre nous et le ciel"),
            Activity.playing("démontrer que l²(N) est un honnête espace de fonctions"),
            Activity.playing("calculer le meilleur angle d'artillerie par Newton-Raphson"),
            Activity.streaming("#PoissonPower", "https://youtu.be/580gEIVVKe8"),
            Activity.listening("les FEZZZZZZZ"),
            Activity.playing("comprendre la doc de Oz2"),
            Activity.playing("à observer les SINFs faire des bêtises (comme d'hab)"),
            Activity.playing("regarder la mousse descendre dans sa chope"),
            Activity.playing("déprimer devant ses résistances grillées et ses transistors brûlés sur son circuit"),
            Activity.playing("avoir pitié de ChatGPT pour tout le travail qu'il doit fournir pendant le blocus des étudiants"),
            Activity.competing("Affond 13h")
    );

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
                      .setActivity(Activity.playing("compter les moutons"))
                      .addEventListeners(commandManager)
                      .build();
        jda.awaitReady();

        registerModules(moduleManager);
        jda.getGuilds().forEach(guild ->
                                        moduleManager.enableModules(
                                                guild.getIdLong(),
                                                Config.getModulesStatus(
                                                                guild.getIdLong(),
                                                                moduleManager.getModuleNames())
                                                        .entrySet()
                                                        .stream()
                                                        .filter(Map.Entry::getValue)
                                                        .map(Map.Entry::getKey)
                                                        .toList()
                                        )
        );
        launchPeriodicStatusUpdate();
    }

    private static void registerModules(ModuleManager moduleManager) {
        List<Class<? extends Module>> modules = Arrays.asList(
                AutoPinModule.class,
                MirrorModule.class,
                QuoteModule.class,
                RssModule.class,
                BasicCommandModule.class,
                ConfessionModule.class,
                ConfigurationModule.class
        );
        List<Command> globalCommands = new ArrayList<>();
        for (Guild guild : jda.getGuilds()) {
            moduleManager.addModules(modules.stream().map(clazz -> {
                try {
                    return clazz.getDeclaredConstructor(Long.class).newInstance(guild.getIdLong());
                } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                         IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).map(o -> (Module)o).peek(m -> {
                if(m.getClass().equals(ConfessionModule.class)) {
                    globalCommands.add(m.getCommands().get(0));
                }
            }).toList());
        }
        commandManager.addGlobalCommands(globalCommands);
    }

    private static void launchPeriodicStatusUpdate() {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        Random random = new Random();
        service.scheduleAtFixedRate(() -> jda.getPresence().setActivity(status.get(random.nextInt(status.size()))),26*6, 26*6, TimeUnit.SECONDS); // 2min30
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
