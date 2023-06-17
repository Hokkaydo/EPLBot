package com.github.hokkaydo.eplbot.module.addcommand;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CustomCommand implements Command {

    private String NAME;
    private String TEXT;

    public CustomCommand(String name, String text) {
        NAME = name;
        TEXT = text;
    }

    @Override
    public void executeCommand(CommandContext context) {
        context.replyCallbackAction().setContent(TEXT).queue();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> TEXT;
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
        return () -> TEXT;
    }
    
}
