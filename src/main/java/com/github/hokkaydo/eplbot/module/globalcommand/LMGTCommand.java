package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LMGTCommand implements Command {

    private static final String LMGT_link = "https://letmegooglethat.com/?q=";
    @Override
    public void executeCommand(CommandContext context) {
        if(context.options().isEmpty()) throw new IllegalStateException("Should not arise");
        String subject = context.options().getFirst().getAsString();
        context.replyCallbackAction().setContent(STR."\{LMGT_link}\{subject}").queue();
    }

    @Override
    public String getName() {
        return "lmgt";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("LMGT_COMMAND_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.STRING, "subject", "Sujet à chercher", true));
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
        return () -> Strings.getString("LMGT_COMMAND_HELP");
    }

}
