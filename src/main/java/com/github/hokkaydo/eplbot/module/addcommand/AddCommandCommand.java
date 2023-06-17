package com.github.hokkaydo.eplbot.module.addcommand;

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

public class AddCommandCommand implements Command{

    private static final String NAME = "name";
    private static final String TEXT = "text";

    private AddCommandManager addCommandManager;

    public AddCommandCommand(AddCommandManager addCommandManager) {
        this.addCommandManager = addCommandManager;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> nameOption = context.options().stream().filter(o -> o.getName().equals(NAME)).findFirst();
        Optional<OptionMapping> textOption = context.options().stream().filter(o -> o.getName().equals(TEXT)).findFirst();
        if(nameOption.isEmpty() || textOption.isEmpty()){
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_ADDCOMMAND_MISSING_ARG")).queue();
            return;
        }
        String name = nameOption.get().getAsString();
        String text = textOption.get().getAsString();
        if(addCommandManager.existsCommand(name)){
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_ADDCOMMAND_ALREADY_EXISTS")).queue();
            return;
        }
        addCommandManager.addCommand(context.author().getGuild(), name, text);
        context.replyCallbackAction().setContent(Strings.getString("COMMAND_ADDCOMMAND_SUCCESS")).queue();
    }

    @Override
    public String getName() {
        return "addcommand";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_ADDCOMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, NAME, Strings.getString("COMMAND_ADDCOMMAND_NAME_OPTION_DESCRIPTION"), true),
            new OptionData(OptionType.STRING, TEXT, Strings.getString("COMMAND_ADDCOMMAND_TEXT_OPTION_DESCRIPTION"), true)
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
        return () -> Strings.getString("COMMAND_ADDCOMMAND_HELP");
    }
    
}
