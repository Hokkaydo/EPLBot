package com.github.hokkaydo.eplbot.module.basic;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ClearFromCommand implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> fromIdOption = context.options().stream().filter(o -> o.getName().equals("from")).findFirst();
        if(fromIdOption.isEmpty()) return;
        context.channel().getHistoryAfter(fromIdOption.get().getAsString(), 100)
                .map(MessageHistory::getRetrievedHistory)
                .queue(l -> l.stream().map(Message::delete).forEach(AuditableRestAction::queue));
        context.channel().deleteMessageById(fromIdOption.get().getAsString()).queue(
                s-> context.replyCallbackAction().setContent(Strings.getString("COMMAND_CLEAR_PROCESSING")).queue(),
                f -> context.replyCallbackAction().setContent(Strings.getString("COMMAND_CLEAR_MESSAGE_TOO_OLD")).queue()
        );
    }

    @Override
    public String getName() {
        return "clearfrom";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CLEAR_FROM_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.STRING, "from", Strings.getString("COMMAND_CLEAR_FROM_OPTION_FROM_DESCRIPTION"), true));
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
        return true;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_CLEAR_FROM_HELP");
    }

}
