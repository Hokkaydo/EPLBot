package com.github.hokkaydo.eplbot.module.eplcommand;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

public class ClearBetween implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> idAOption = context.options().stream().filter(o -> o.getName().equals("id_a")).findFirst();
        Optional<OptionMapping> idBOption = context.options().stream().filter(o -> o.getName().equals("id_b")).findFirst();
        if(idAOption.isEmpty() || idBOption.isEmpty()) return;

        context.channel().getHistoryAfter(idAOption.get().getAsString(), 100)
                .map(MessageHistory::getRetrievedHistory)
                .queue(l -> {
                    SortedSet<Message> sorted = new TreeSet<>(Comparator.comparing(ISnowflake::getTimeCreated));
                    sorted.addAll(l);
                    for (Message message : sorted) {
                        message.delete().queue();
                        if(message.getId().equals(idBOption.get().getAsString())) return;
                    }
                });
        context.channel().deleteMessageById(idAOption.get().getAsString()).queue(
                s -> context.replyCallbackAction().setContent(Strings.getString("COMMAND_CLEAR_PROCESSING")).queue(),
                f -> context.replyCallbackAction().setContent(Strings.getString("COMMAND_CLEAR_MESSAGE_TOO_OLD")).queue()
        );

    }

    @Override
    public String getName() {
        return "clearbetween";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CLEAR_BETWEEN_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "id_a", Strings.getString("COMMAND_CLEAR_BETWEEN_OPTION_ID_A_DESCRIPTION"), true),
                new OptionData(OptionType.STRING, "id_b", Strings.getString("COMMAND_CLEAR_BETWEEN_OPTION_ID_B_DESCRIPTION"), true)
        );
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
        return () -> Strings.getString("COMMAND_CLEAR_BETWEEN_HELP");
    }

}
