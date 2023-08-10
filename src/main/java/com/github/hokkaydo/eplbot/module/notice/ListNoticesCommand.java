package com.github.hokkaydo.eplbot.module.notice;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ListNoticesCommand extends ListenerAdapter implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        //TODO complete notice command execution
    }

    @Override
    public String getName() {
        return "listnotices";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_LIST_NOTICES_DESCRIPTION");
    }

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
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_LIST_NOTICES_HELP");
    }

}
