package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandManager;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.ModuleManager;
import com.github.hokkaydo.eplbot.module.autopin.AutoPinModule;
import com.github.hokkaydo.eplbot.module.christmas.ChristmasModule;
import com.github.hokkaydo.eplbot.module.confession.ConfessionModule;
import com.github.hokkaydo.eplbot.module.earlybird.EarlyBirdModule;
import com.github.hokkaydo.eplbot.module.eplcommand.EPLCommandModule;
import com.github.hokkaydo.eplbot.module.globalcommand.GlobalCommandModule;
import com.github.hokkaydo.eplbot.module.graderetrieve.ExamsRetrieveModule;
import com.github.hokkaydo.eplbot.module.mirror.MirrorModule;
import com.github.hokkaydo.eplbot.module.notice.NoticeModule;
import com.github.hokkaydo.eplbot.module.quote.QuoteModule;
import com.github.hokkaydo.eplbot.module.ratio.RatioModule;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {

    private static JDA jda;
    private static ModuleManager moduleManager;
    private static CommandManager commandManager;
    public static final Long EPL_DISCORD_ID = 517720163223601153L;
    private static Long prodDiscordId = 0L;
    public static final String PERSISTENCE_DIR_PATH = "./persistence";
    private static final Random RANDOM = new Random();
    public static final Logger LOGGER = Logger.getLogger("EPLBot");
    private static List<Long> specialDiscordIds;

    private static final List<Activity> status = List.of(
            Activity.playing("bâtir des ponts (solides) entre nous et le ciel"),
            Activity.playing("démontrer que l²(N) est un honnête espace de fonctions"),
            Activity.playing("calculer le meilleur angle d'artillerie par Newton-Raphson"),
            Activity.of(Activity.ActivityType.LISTENING, "@POISSON?!", "https://youtu.be/580gEIVVKe8"),
            Activity.of(Activity.ActivityType.LISTENING, "les FEZZZZZZZ", "https://www.youtube.com/watch?v=KUDJOsaAFOs"),
            Activity.playing("comprendre la doc de Oz2"),
            Activity.playing("à observer les SINFs faire des bêtises (comme d'hab)"),
            Activity.playing("regarder la mousse descendre dans sa chope"),
            Activity.playing("griller les amplis op et brûler les transistors de son circuit"),
            Activity.playing("avoir pitié de ChatGPT pour tout le travail qu'il doit fournir pendant le blocus des étudiants"),
            Activity.playing("conspire avec les modos"),
            Activity.playing("faire la pédicure des Rois"),
            Activity.playing("chercher un modèle DB adapté pour stocker votre égo"),
            Activity.playing("relire toutes les confessions"),
            Activity.playing("Please wait, your messages are being sent to UCLouvain ..."),
            Activity.playing("chercher du sens sous la place des Sciences"),
            Activity.of(Activity.ActivityType.LISTENING, "Apocalypse894", "https://open.spotify.com/track/0A6FdQB9XVIbjP6Kr4vsa1?si=8cbfc79518df45d1"),
            Activity.competing("Affond 13h"),
            Activity.competing("un beerpong"),
            Activity.competing("Procrastination"),
            Activity.of(Activity.ActivityType.STREAMING, "Radio Gazou", "https://www.youtube.com/watch?v=rj_kEDituic")
    );

    public static void main(String[] args) throws InterruptedException, IOException {
        launch(args);
    }

    private static void launch(String[] args) throws InterruptedException, IOException {
        LOGGER.log(Level.INFO, "--------- START ---------");
        String token = System.getenv("DISCORD_BOT_TOKEN");
        String testDiscordIdStr = System.getenv("TEST_DISCORD_ID");
        Long testDiscordId = testDiscordIdStr == null ? 1108141461498777722L : Long.parseLong(testDiscordIdStr);
        prodDiscordId = testDiscordIdStr == null ? EPL_DISCORD_ID : testDiscordId;
        specialDiscordIds = List.of(prodDiscordId, testDiscordId);

        if (token == null && args.length > 0) token = args[0];
        if (token == null) throw new IllegalStateException("No token specified !");
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        DatabaseManager.initialize(PERSISTENCE_DIR_PATH);
        DatabaseManager.regenerateDatabase(false);
        final GuildStateListener guildStateListener = new GuildStateListener();
        Path path = Path.of(Main.PERSISTENCE_DIR_PATH);
        if (!Files.exists(path))
            Files.createDirectory(path);

        Config.load();
        Strings.load();
        jda = JDABuilder.createDefault(token)
                      .enableIntents(EnumSet.allOf(GatewayIntent.class))
                      .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                      .setBulkDeleteSplittingEnabled(false)
                      .setActivity(Activity.playing("compter les moutons"))
                      .addEventListeners(commandManager, guildStateListener)
                      .build();
        jda.awaitReady();

        registerModules();
        jda.getGuilds().forEach(guild -> {
                    List<String> modules = Config.getModulesStatuses(
                                    guild.getIdLong(),
                                    moduleManager.getModuleNames()
                            )
                                                   .entrySet()
                                                   .stream()
                                                   .filter(Map.Entry::getValue)
                                                   .map(Map.Entry::getKey)
                                                   .toList();
                    StringBuilder log = new StringBuilder("Registering modules for %s :%n".formatted(guild.getName()));
                    for (String module : modules) {
                        log.append("\t%s%n".formatted(module));
                    }
                    String logS = log.toString();
                    LOGGER.log(Level.INFO, logS);
                    moduleManager.enableModules(guild.getIdLong(), modules);
                }
        );
        launchPeriodicStatusUpdate();
    }

    protected static final List<Long> globalModuleRegisteredGuilds = new ArrayList<>();
    protected static final List<Long> eplModuleRegisteredGuilds = new ArrayList<>();

    public static void registerModules() {
        List<Class<? extends Module>> globalModules = Arrays.asList(
                MirrorModule.class,
                GlobalCommandModule.class,
                QuoteModule.class,
                RssModule.class,
                AutoPinModule.class,
                RssModule.class,
                NoticeModule.class
        );
        List<Class<? extends Module>> eplModules = Arrays.asList(
                EPLCommandModule.class,
                ConfessionModule.class,
                ExamsRetrieveModule.class,
                RatioModule.class,
                EarlyBirdModule.class,
                ChristmasModule.class
        );
        Map<Long, List<Command>> guildCommands = new HashMap<>();
        for (Long guildId : specialDiscordIds) {
            if (eplModuleRegisteredGuilds.contains(guildId)) continue;
            eplModuleRegisteredGuilds.add(guildId);
            guildCommands.put(guildId, new ArrayList<>());
            List<Module> modules = eplModules.stream()
                                           .map(clazz -> instantiate(clazz, guildId))
                                           .map(o -> (Module) o)
                                           .toList();

            modules.forEach(m -> guildCommands.get(guildId).addAll(m.getCommands()));
            moduleManager.addModules(modules);
        }

        List<Long> guildIds = new ArrayList<>(jda.getGuilds().stream().map(Guild::getIdLong).toList());
        guildIds.addAll(specialDiscordIds);

        for (Long guildId : guildIds) {
            if (globalModuleRegisteredGuilds.contains(guildId)) continue;
            if (!guildCommands.containsKey(guildId)) {
                guildCommands.put(guildId, new ArrayList<>());
            }
            globalModuleRegisteredGuilds.add(guildId);
            List<Module> modules = globalModules.stream()
                                           .map(clazz -> instantiate(clazz, guildId))
                                           .map(o -> (Module) o)
                                           .toList();

            modules.forEach(m -> guildCommands.get(guildId).addAll(m.getCommands()));
            moduleManager.addModules(modules);
        }

        for (Map.Entry<Long, List<Command>> guildListEntry : guildCommands.entrySet()) {
            Guild guild = jda.getGuildById(guildListEntry.getKey());
            if (guild == null) continue;
            Main.getCommandManager().addCommands(guild, guildListEntry.getValue());
        }
        getModuleManager().getModuleByName("confession", prodDiscordId, ConfessionModule.class).ifPresent(m -> {
            commandManager.addGlobalCommands(m.getGlobalCommands());
            commandManager.enableGlobalCommands((m.getGlobalCommands().stream().map(Command::getClass).collect(Collectors.toList())));
        });
    }

    private static <T> T instantiate(Class<T> clazz, Long guildId) {
        try {
            return clazz.getDeclaredConstructor(Long.class).newInstance(guildId);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void launchPeriodicStatusUpdate() {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(() -> jda.getPresence().setActivity(status.get(RANDOM.nextInt(status.size()))), 10L, 26 * 6L, TimeUnit.SECONDS); // 2min30
    }

    public static JDA getJDA() {
        return Main.jda;
    }

    public static ModuleManager getModuleManager() {
        return Main.moduleManager;
    }

    public static CommandManager getCommandManager() {
        return Main.commandManager;
    }

}