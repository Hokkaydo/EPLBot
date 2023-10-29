package com.github.hokkaydo.eplbot.module.addcommand;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class RemoveCommandCommand implements Command{

    private static final String NAME = "name";

    private CustomCommandManager customCommandManager;

    public RemoveCommandCommand(CustomCommandManager customCommandManager) {
        this.customCommandManager = customCommandManager;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> nameOption = context.options().stream().filter(o -> o.getName().equals(NAME)).findFirst();
        if(nameOption.isEmpty()){
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_ADDCOMMAND_MISSING_ARG")).queue();
            return;
        }
        String name = nameOption.get().getAsString();
        if(!customCommandManager.existsCommand(name)){
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_REMOVECOMMAND_NOT_EXISTS")).queue();
            return;
        }
        customCommandManager.removeCommand(context.author(), name);
        context.replyCallbackAction().setContent(Strings.getString("COMMAND_REMOVECOMMAND_SUCCESS")).queue();
    }

    @Override
    public String getName() {
        return "removecommand";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_REMOVECOMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, NAME, Strings.getString("COMMAND_ADDCOMMAND_NAME_OPTION_DESCRIPTION"), true)
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
        return () -> Strings.getString("COMMAND_REMOVECOMMAND_HELP");
    }
    
}
