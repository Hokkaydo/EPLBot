package com.github.hokkaydo.eplbot.module.eplcommand;

import com.github.hokkaydo.eplbot.CRUDRepository;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.repository.ConfigurationRepositorySQLite;
import com.github.hokkaydo.eplbot.module.confession.repository.WarnedConfessionRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepositorySQLite;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DebugCommand implements Command {

    private static final Consumer<PrivateChannel> DEFAULT = c -> c.sendMessage("Unknown subcommand").queue();
    private static final Map<String, Consumer<PrivateChannel>> SUB_COMMANDS = Map.of(
            "dump_db", DebugCommand::dumpDB,
            "dump_errors", DebugCommand::dumpErrors,
            "dump", DebugCommand::dump,
            "regenerate_db", DebugCommand::regenerateDB
    );

    private static final CourseRepositorySQLite courseRepo = new CourseRepositorySQLite(Main.getDataSource());

    private static final List<CRUDRepository<?>> repositories = List.of(
            courseRepo,
            new CourseGroupRepositorySQLite(Main.getDataSource(), courseRepo),
            new WarnedConfessionRepositorySQLite(Main.getDataSource()),
            new ConfigurationRepositorySQLite(Main.getDataSource())
    );

    @Override
    public void executeCommand(CommandContext context) {
        String sub = context.options().get(0).getAsString();
        context.author().getUser().openPrivateChannel().queue(c -> SUB_COMMANDS.getOrDefault(sub, DEFAULT).accept(c));
        context.replyCallbackAction().setContent("Sent !").queue();
    }

    private static void dumpDB(PrivateChannel channel) {
        channel.sendMessage("--------------- DUMP DATABASE ---------------\n").queue();
        for (CRUDRepository<?> repository : repositories) {
            StringBuilder s = new StringBuilder();
            for (Object o : repository.readAll()) {
                s.append(o).append("\n");
            }
            channel.sendMessage(s).queue();
        }
    }

    private static void regenerateDB(PrivateChannel channel) {
        //TODO
    }

    private static void dumpErrors(PrivateChannel channel) {
        //TODO
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
