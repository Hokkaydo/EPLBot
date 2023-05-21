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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    private static Long TEST_DISCORD_ID = 1108141461498777722L;
    private static final Long SINF_DISCORD_ID = 492762354111479828L;
    public static String PERSISTENCE_DIR_PATH = "./persistence";

    private static final List<Activity> status = List.of(
            Activity.playing("bâtir des ponts (solides) entre nous et le ciel"),
            Activity.playing("démontrer que l²(N) est un honnête espace de fonctions"),
            Activity.playing("calculer le meilleur angle d'artillerie par Newton-Raphson"),
            Activity.of(Activity.ActivityType.LISTENING,"@POISSON?!", "https://youtu.be/580gEIVVKe8"),
            Activity.of(Activity.ActivityType.LISTENING, "les FEZZZZZZZ", "https://www.youtube.com/watch?v=KUDJOsaAFOs"),
            Activity.playing("comprendre la doc de Oz2"),
            Activity.playing("à observer les SINFs faire des bêtises (comme d'hab)"),
            Activity.playing("regarder la mousse descendre dans sa chope"),
            Activity.playing("griller les amplis op et brûler les transistors de son circuit"),
            Activity.playing("avoir pitié de ChatGPT pour tout le travail qu'il doit fournir pendant le blocus des étudiants"),
            Activity.competing("Affond 13h"),
            Activity.of(Activity.ActivityType.STREAMING, "Radio Gazou", "https://www.youtube.com/watch?v=rj_kEDituic")
    );

    public static void main(String[] args) throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        String token = System.getenv("DISCORD_BOT_TOKEN");
        String testDiscordId = System.getenv("TEST_DISCORD_ID");
        TEST_DISCORD_ID = testDiscordId == null ? 1108141461498777722L : Long.parseLong(testDiscordId);
        if(token == null) token = args[0];
        if(token == null) throw new IllegalStateException("No token specified !");
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        Path path = Path.of(Main.PERSISTENCE_DIR_PATH);
        if(!Files.exists(path))
            Files.createDirectory(path);

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
        Map<Guild, List<Command>> guildCommands = new HashMap<>();
        for(Long guildId : List.of(EPL_DISCORD_ID, TEST_DISCORD_ID)) {
            Guild guild = jda.getGuildById(guildId);
            if(guild == null) continue;
            guildCommands.put(guild, new ArrayList<>());
            moduleManager.addModules(eplModules.stream()
                                             .map(clazz -> instantiate(clazz, guildId))
                                             .map(o -> (Module)o)
                                             .peek(m -> {
                                                         if(!m.getClass().equals(ConfessionModule.class)) {
                                                             guildCommands.get(guild).addAll(m.getCommands());
                                                         }
                                                     }
                                             ).toList()
            );
        }

        for (Long guildId : List.of(EPL_DISCORD_ID, TEST_DISCORD_ID, SINF_DISCORD_ID)) {
            Guild guild = jda.getGuildById(guildId);
            if(guild == null) continue;
            moduleManager.addModules(globalModules.stream()
                                             .map(clazz -> instantiate(clazz, guildId))
                                             .map(o -> (Module)o)
                                             .peek(m -> System.out.println("\t" + m.getName()))
                                             .peek(m -> guildCommands.get(guild).addAll(m.getCommands()))
                                             .toList()
            );
            System.out.println("\n");
        }
        for (Map.Entry<Guild, List<Command>> guildListEntry : guildCommands.entrySet()) {
            Main.getCommandManager().addCommands(guildListEntry.getKey(), guildListEntry.getValue());
        }
        getModuleManager().getModuleByName("confession", TEST_DISCORD_ID, ConfessionModule.class).ifPresent(m -> commandManager.addGlobalCommands(m.getCommands()));
        getModuleManager().getModule(MirrorModule.class).forEach(MirrorModule::loadMirrors);
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
