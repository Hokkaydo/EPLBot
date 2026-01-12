package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.command.CommandManager;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.ModuleManager;
import com.github.hokkaydo.eplbot.module.autopin.AutoPinModule;
import com.github.hokkaydo.eplbot.module.bookmark.BookMarkModule;
import com.github.hokkaydo.eplbot.module.christmas.ChristmasModule;
import com.github.hokkaydo.eplbot.module.code.CodeModule;
import com.github.hokkaydo.eplbot.module.confession.ConfessionModule;
import com.github.hokkaydo.eplbot.module.data.DataModule;
import com.github.hokkaydo.eplbot.module.eplcommand.EPLCommandModule;
import com.github.hokkaydo.eplbot.module.globalcommand.GlobalCommandModule;
import com.github.hokkaydo.eplbot.module.graderetrieve.ExamsRetrieveModule;
import com.github.hokkaydo.eplbot.module.helper.HelperModule;
import com.github.hokkaydo.eplbot.module.menu.MenuModule;
import com.github.hokkaydo.eplbot.module.messagebird.MessageBirdModule;
import com.github.hokkaydo.eplbot.module.mirror.MirrorModule;
import com.github.hokkaydo.eplbot.module.notice.NoticeModule;
import com.github.hokkaydo.eplbot.module.quote.QuoteModule;
import com.github.hokkaydo.eplbot.module.ratio.RatioModule;
import com.github.hokkaydo.eplbot.module.rss.RssModule;
import com.github.hokkaydo.eplbot.module.status.StatusModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static JDA jda;
    private static ModuleManager moduleManager;
    private static CommandManager commandManager;
    private static Long bossId = 0L;
    public static final String PERSISTENCE_DIR_PATH = "./persistence";
    private static final Random RANDOM = new Random();
    public static final Logger LOGGER = JDALogger.getLog(Main.class);

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

    private static final List<Class<? extends Module>> MODULES = Arrays.asList(
            MirrorModule.class,
            GlobalCommandModule.class,
            QuoteModule.class,
            RssModule.class,
            AutoPinModule.class,
            RssModule.class,
            NoticeModule.class,
            BookMarkModule.class,
            HelperModule.class,
            MenuModule.class,
            CodeModule.class,
            MessageBirdModule.class,
            ConfessionModule.class,
            ExamsRetrieveModule.class,
            RatioModule.class,
            ChristmasModule.class,
            EPLCommandModule.class,
            DataModule.class,
            StatusModule.class
    );

    public static void main(String[] args) throws InterruptedException, IOException {
        launch(args);
    }

    private static void launch(String[] args) throws InterruptedException, IOException {
        LOGGER.info("--------- START ---------");
        String token = System.getenv("DISCORD_BOT_TOKEN");
        String bossIdStr = System.getenv("BOSS_ID");
        if (bossIdStr != null) bossId = Long.parseLong(bossIdStr);

        if (token == null && args.length > 0) token = args[0];
        if (token == null) throw new IllegalStateException("No token specified!");
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
                      .enableCache(CacheFlag.MEMBER_OVERRIDES,CacheFlag.ROLE_TAGS)
                      .setMemberCachePolicy(MemberCachePolicy.ALL)
                      .disableCache(CacheFlag.VOICE_STATE)
                      .setBulkDeleteSplittingEnabled(false)
                      .setActivity(Activity.playing("compter les moutons"))
                      .addEventListeners(commandManager, guildStateListener)
                      .build();
        jda.awaitReady();

        jda.getGuilds().stream().map(Guild::getIdLong).forEach(Main::registerModules);
        launchPeriodicStatusUpdate();
    }

    public static void registerModules(Long guildId) {
        List<Module> instantiation = MODULES.stream()
                                             .map(clazz -> instantiate(clazz, guildId))
                                             .map(o -> (Module) o)
                                             .toList();
        instantiation.stream().map(Module::getCommands).forEach(commands -> getCommandManager().addCommands(guildId, commands));
        moduleManager.addModules(instantiation);


        List<String> modules = Config.getModulesStatuses(
                        guildId,
                        moduleManager.getModuleNames()
                )
                                       .entrySet()
                                       .stream()
                                       .filter(Map.Entry::getValue)
                                       .map(Map.Entry::getKey)
                                       .toList();
        moduleManager.enableModules(guildId, modules);

        String logStr = "Registering modules for %s: ".formatted(Optional.ofNullable(jda.getGuildById(guildId)).map(Guild::getName).orElse("Unknown")) +
                                modules.stream().reduce("", (s, s2) -> s + ", " + s2).replaceFirst(", ", "");
        LOGGER.info(logStr);
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

    public static CommandManager getCommandManager() {
        return commandManager;
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static JDA getJDA() {
        return jda;
    }

    public static Long getBossId() {
        return bossId;
    }

}