package com.github.hokkaydo.eplbot.module.eplcommand;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class MoveMessagesCommand implements Command {

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> idAOption = context.options().stream().filter(o -> o.getName().equals("id_a")).findFirst();
        Optional<OptionMapping> idBOption = context.options().stream().filter(o -> o.getName().equals("id_b")).findFirst();
        Optional<OptionMapping> channelOption = context.options().stream().filter(o -> o.getName().equals("channel")).findFirst();
        if(idAOption.isEmpty() || idBOption.isEmpty() || channelOption.isEmpty()) return;
        long idA = idAOption.get().getAsLong();
        long idB = idBOption.get().getAsLong();
        TextChannel newChannel = channelOption.get().getAsChannel().asTextChannel();
        context.replyCallbackAction().setContent("Processing ...").queue();
        context.channel().retrieveMessageById(idA).queue(m -> context.channel().getHistoryAfter(idA, 100).queue(history -> parseHistory(m, idB, history, newChannel, context)));
    }

    private void parseHistory(Message m, long idB, MessageHistory history, TextChannel newChannel, CommandContext context) {
        List<Message> sortedMessages = new ArrayList<>(history.getRetrievedHistory());
        sortedMessages.add(m);
        sortedMessages.sort((a, b) -> OffsetDateTime.timeLineOrder().compare(a.getTimeCreated(), b.getTimeCreated()));
        int endIndex = -1;
        for (int i = 0; i < sortedMessages.size(); i++) {
            if(sortedMessages.get(i).getIdLong() == idB) {
                endIndex = i;
                break;
            }
        }
        if(endIndex != -1) {
            sortedMessages.removeAll(sortedMessages.subList(endIndex+1, sortedMessages.size()));
        }
        for (int i = 0; i < sortedMessages.size(); i++) {
            Message message = sortedMessages.get(i);
            MessageEmbed embed = MessageUtil.toEmbed(message).build();
            if(i == sortedMessages.size() - 1) {
                newChannel.sendMessageEmbeds(embed).and(message.delete()).queue(s -> sendMovedAmountMessage(context, sortedMessages.size()));
                break;
            }
            newChannel.sendMessageEmbeds(embed).queue(s -> message.delete().queue());
        }
    }

    private void sendMovedAmountMessage(CommandContext context, int amount) {
        context.hook().sendMessage(String.format(Strings.getString("COMMAND_MOVE_MESSAGES_MOVED"), amount)).queue();
    }

    @Override
    public String getName() {
        return "movemessages";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_MOVE_MESSAGES_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "id_a", Strings.getString("COMMAND_MOVE_MESSAGES_OPTION_ID_A_DESCRIPTION"), true),
                new OptionData(OptionType.STRING, "id_b", Strings.getString("COMMAND_MOVE_MESSAGES_OPTION_ID_B_DESCRIPTION"), true),
                new OptionData(OptionType.CHANNEL, "channel", Strings.getString("COMMAND_MOVE_MESSAGES_OPTION_CHANNEL_DESCRIPTION"), true)
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
        return () -> Strings.getString("COMMAND_MOVE_MESSAGES_HELP");
    }

}
