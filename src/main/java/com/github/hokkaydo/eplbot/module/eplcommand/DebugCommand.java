package com.github.hokkaydo.eplbot.module.eplcommand;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.repository.ConfigurationRepositorySQLite;
import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.confession.repository.WarnedConfessionRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.ExamRetrieveThreadRepositorySQLite;
import com.github.hokkaydo.eplbot.module.mirror.repository.MirrorLinkRepositorySQLite;
import com.github.hokkaydo.eplbot.module.notice.repository.NoticeRepositorySQLite;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DebugCommand implements Command {

    private static final Consumer<PrivateChannel> DEFAULT = c -> c.sendMessage("Unknown subcommand").queue();
    private static final int HASTEBIN_MAX_CONTENT_LENGTH = 350_000;
    private static final Map<String, Consumer<PrivateChannel>> SUB_COMMANDS = Map.of(
            "dump_db", DebugCommand::dumpDB,
            "dump_errors", DebugCommand::dumpErrors,
            "dump", DebugCommand::dump,
            "regenerate_db", DebugCommand::regenerateDB
    );

    private static final CourseRepositorySQLite courseRepo = new CourseRepositorySQLite(DatabaseManager.getDataSource());
    private static final CourseGroupRepositorySQLite groupRepo = new CourseGroupRepositorySQLite(DatabaseManager.getDataSource(), courseRepo);

    private static final List<CRUDRepository<?>> repositories = List.of(
            courseRepo,
            groupRepo,
            new WarnedConfessionRepositorySQLite(DatabaseManager.getDataSource()),
            new ConfigurationRepositorySQLite(DatabaseManager.getDataSource()),
            new MirrorLinkRepositorySQLite(DatabaseManager.getDataSource()),
            new ExamRetrieveThreadRepositorySQLite(DatabaseManager.getDataSource()),
            new NoticeRepositorySQLite(courseRepo, groupRepo)
    );

    private static void dumpDB(PrivateChannel channel) {
        HttpClient client = HttpClient.newHttpClient();
        Map<String, List<String>> output = new HashMap<>();

        List<CompletableFuture<Void>> requests = new ArrayList<>();

        for (CRUDRepository<?> repository : repositories) {
            output.computeIfAbsent(repository.getClass().getSimpleName(), _ -> new ArrayList<>());
            StringBuilder s = new StringBuilder();

            for (Object o : repository.readAll()) {
                s.append(o).append("\n");
            }
            if(s.toString().isBlank()) continue;
            String content = s.toString();
            while(content.length() > HASTEBIN_MAX_CONTENT_LENGTH) {
                requests.add(MessageUtil.hastebinPost(client, content.substring(0, HASTEBIN_MAX_CONTENT_LENGTH))
                                     .thenAccept(link -> output.get(repository.getClass().getSimpleName()).add(link)));
                content = content.substring(HASTEBIN_MAX_CONTENT_LENGTH);
            }
            requests.add(MessageUtil.hastebinPost(client, content).thenAccept(link -> output.get(repository.getClass().getSimpleName()).add(link)));

        }
        requests.forEach(CompletableFuture::join);
        sendDBDumpMessage(channel, output);
    }

    private static void sendDBDumpMessage(PrivateChannel channel, Map<String, List<String>> output) {
        StringBuilder message = new StringBuilder("--------------- DUMP DATABASE ---------------\n");
        for (Map.Entry<String, List<String>> entry : output.entrySet()) {
            String padding = " ".repeat(20);
            message
                    .append("- ")
                    .append(entry.getKey())
                    .append(padding)
                    .append(listToString(entry.getValue()))
                    .append("\n");
        }
        message.append("---------------------------------------------");
        channel.sendMessage(message).queue();
    }

    private static String listToString(List<String> list) {
        if(list.isEmpty()) return "Empty repository";
        String first = list.getFirst();
        list.removeFirst();
        return first + list.stream().reduce("", (a,b) -> STR."\{a} - \{b}");
    }

    @Override
    public void executeCommand(CommandContext context) {
        String sub = context.options().getFirst().getAsString();
        context.author().getUser().openPrivateChannel().queue(c -> SUB_COMMANDS.getOrDefault(sub, DEFAULT).accept(c));
        context.replyCallbackAction().setContent("Done !").queue();
    }


    private static void regenerateDB(PrivateChannel channel) {
        DatabaseManager.regenerateDatabase(true);
    }

    private static void dumpErrors(PrivateChannel channel) {
        // TODO when implementing better slogging
    }

    private static void dump(PrivateChannel channel) {
        dumpDB(channel);
        dumpErrors(channel);
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> "Debug ONLY !";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, "subcommand", "Subcommand to execute", true)
                               .addChoice("dump_db", "dump_db")
                               .addChoice("dump_errors", "dump_errors")
                               .addChoice("dump", "dump")
                               .addChoice("regenerate_db", "regenerate_db")
        );
    }

    @Override
    public boolean ephemeralReply() {
        return false;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public Supplier<String> help() {
        return () -> "DEBUG ONLY";
    }

}
