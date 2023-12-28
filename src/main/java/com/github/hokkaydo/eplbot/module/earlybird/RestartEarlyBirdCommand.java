package com.github.hokkaydo.eplbot.module.earlybird;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class RestartEarlyBirdCommand implements Command {

    private final EarlyBirdListener listener;

    RestartEarlyBirdCommand(EarlyBirdListener listener) {
        this.listener = listener;
    }
    @Override
    public void executeCommand(CommandContext context) {
        listener.restart();
        context.replyCallbackAction().setContent("Restarted !").queue();
    }

    @Override
    public String getName() {
        return "earlybirdrestart";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("EARLY_BIRD_RESTART_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
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
        return () -> Strings.getString("EARLY_BIRD_RESTART_COMMAND_HELP");
    }

}
