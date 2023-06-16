package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ClearConfessWarningsCommand implements Command {

    private final ConfessionProcessor processor;
    public ClearConfessWarningsCommand(ConfessionProcessor processor) {
        this.processor = processor;
    }
    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> userOpt = context.options().stream().filter(o -> o.getName().equals("user")).findFirst();
        if(userOpt.isEmpty()) return;
        User user = userOpt.get().getAsUser();
        processor.clearWarnings(user.getIdLong());
        context.replyCallbackAction().setContent(Strings.getString("COMMAND_CLEARCONFESSWARNINGS_DONE")).queue();
    }

    @Override
    public String getName() {
        return "clearconfesswarnings";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CLEARCONFESSWARNINGS_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.USER, "user", Strings.getString("COMMAND_CLEARCONFESSWARNINGS_OPTION_USER_DESCRIPTION"), true));
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
        return () -> Strings.getString("COMMAND_CLEARCONFESSWARNINGS_HELP");
    }

}
