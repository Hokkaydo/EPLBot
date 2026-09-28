package com.github.hokkaydo.eplbot.module.contributions;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.contributions.DriveLister.DriveFile;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lists the files still waiting in the contributions folder, i.e. not imported yet.
 * */
public class ContributionsCommand implements Command {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    @Override
    public void executeCommand(CommandContext context) {
        // Listing the Drive takes a few seconds, acknowledge the interaction first
        context.replyCallbackAction().queue();
        context.hook().setEphemeral(true);
        Thread.ofVirtual().start(() -> {
            try {
                reply(context, DriveLister.list());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Any failure must end the "thinking" state of the interaction
                Main.LOGGER.error("[Contributions] Error while listing contributions", e);
                context.hook().editOriginal(Strings.getString("contributions.command.error")).queue();
            }
        });
    }

    private static void reply(CommandContext context, List<DriveFile> files) {
        if (files.isEmpty()) {
            context.hook().editOriginal(Strings.getString("contributions.command.empty")).queue();
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add(Strings.getString("contributions.command.header").formatted(files.size()));
        files.stream().map(f -> "• %s (%s)".formatted(f.displayPath(), f.displaySize())).forEach(lines::add);
        List<String> messages = splitMessages(lines);
        context.hook().editOriginal(messages.getFirst()).queue();
        messages.stream().skip(1).forEach(m -> context.hook().sendMessage(m).queue());
    }

    private static List<String> splitMessages(List<String> lines) {
        List<String> messages = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (line.length() > MAX_MESSAGE_LENGTH) line = line.substring(0, MAX_MESSAGE_LENGTH - 1) + "…";
            if (!current.isEmpty() && current.length() + 1 + line.length() > MAX_MESSAGE_LENGTH) {
                messages.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) current.append('\n');
            current.append(line);
        }
        if (!current.isEmpty()) messages.add(current.toString());
        return messages;
    }

    @Override
    public String getName() {
        return "contributions";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("contributions.command.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public boolean ephemeralReply() {
        return true;
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
        return () -> Strings.getString("contributions.command.help");
    }

}
