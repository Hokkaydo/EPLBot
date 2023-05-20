package com.github.hokkaydo.eplbot.module.basic;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class PingCommand implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        Main.getJDA().getRestPing().queue(t -> context.replyCallbackAction().setContent(":ping_pong: %dms".formatted(t)).queue());
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_PING_DESCRIPTION");
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
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_PING_HELP");
    }

}
