package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ConfessionFollowCommand implements Command {

    private final Map<Long, Long> lastConfession;

    private final ConfessionProcessor processor;
    public ConfessionFollowCommand(Map<Long, Long> lastConfession, ConfessionProcessor processor) {
        this.lastConfession = lastConfession;
        this.processor = processor;
    }
    @Override
    public void executeCommand(CommandContext context) {
        if(!lastConfession.containsKey(context.user().getIdLong())) {
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_CONFESSIONCONTINUE_NO_LAST_CONFESSION_FOUND")).queue();
            return;
        }
        processor.process(context, true);
    }

    @Override
    public String getName() {
        return "confessfollow";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CONFESSIONCONTINUE_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.STRING, "confession", Strings.getString("COMMAND_CONFESSION_OPTION_DESCRIPTION"), true));
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
        return () -> Strings.getString("COMMAND_CONFESSIONCONTINUE_HELP");
    }

    public ConfessionProcessor getProcessor() {
        return processor;
    }

}
