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
    private static final Long EPL_DISCORD_ID = 517720163223601153L;
    private static final Long TEST_DISCORD_ID = 1108141461498777722L;
    private static final Long SINF_DISCORD_ID = 492762354111479828L;

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
        String token = null;
        if(args.length > 0) token = args[0];
        if(token == null) token = Dotenv.load().get("DISCORD_BOT_TOKEN");
        if(token == null) throw new IllegalStateException("No token specified !");
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
        List<Class<? extends Module>> globalModules = Arrays.asList(
                MirrorModule.class,
                ConfigurationModule.class
        );
        List<Class<? extends Module>> eplModules = Arrays.asList(
                QuoteModule.class,
                RssModule.class,
                BasicCommandModule.class,
                ConfessionModule.class,
                AutoPinModule.class
        );
        for(Long guildId : List.of(EPL_DISCORD_ID, TEST_DISCORD_ID)) {
            Guild guild = jda.getGuildById(guildId);
            if(guild == null) continue;
            System.out.println("Registering EPL modules for " + guild.getName());
            List<Command> guildCommands = new ArrayList<>();
            moduleManager.addModules(eplModules.stream()
                                             .map(clazz -> instantiate(clazz, guildId))
                                             .map(o -> (Module)o)
                                             .peek(m -> {
                                                 System.out.println("\t" + m.getName());
                                                         if(!m.getClass().equals(ConfessionModule.class)) {
                                                             guildCommands.addAll(m.getCommands());
                                                         }
                                                     }
                                             ).toList()
            );
            Main.getCommandManager().addCommands(guildId, guildCommands);
            System.out.println("\n");
        }

        for (Long guildId : List.of(EPL_DISCORD_ID, TEST_DISCORD_ID, SINF_DISCORD_ID)) {
            Guild guild = jda.getGuildById(guildId);
            if(guild == null) continue;
            System.out.println("Registering global modules for " + guild.getName());
            List<Command> guildCommands = new ArrayList<>();
            moduleManager.addModules(globalModules.stream()
                                             .map(clazz -> instantiate(clazz, guildId))
                                             .map(o -> (Module)o)
                                             .peek(m -> System.out.println("\t" + m.getName()))
                                             .peek(m -> guildCommands.addAll(m.getCommands()))
                                             .toList()
            );
            Main.getCommandManager().addCommands(guildId, guildCommands);
            System.out.println("\n");
        }
        getModuleManager().getModuleByName("confession", EPL_DISCORD_ID, ConfessionModule.class).ifPresent(m -> commandManager.addGlobalCommands(m.getCommands()));

    }

    private static <T> T instantiate(Class<T> clazz, Long guildId) {
        try {
            return clazz.getDeclaredConstructor(Long.class).newInstance(guildId);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
