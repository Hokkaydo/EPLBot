package com.github.hokkaydo.eplbot.command;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.function.Supplier;

public interface Command  {

    void executeCommand(CommandContext context);
    String getName();
    Supplier<String> getDescription();
    List<OptionData> getOptions();
    boolean ephemeralReply();
    boolean validateChannel(MessageChannel channel);
    boolean adminOnly();
    Supplier<String> help();

}