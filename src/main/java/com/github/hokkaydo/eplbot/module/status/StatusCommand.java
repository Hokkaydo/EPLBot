package com.github.hokkaydo.eplbot.module.status;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.data.DataRepository;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class StatusCommand implements Command {

    private final DataRepository dataRepository;

    public StatusCommand(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void executeCommand(CommandContext context) {
        int durationDays = 1;
        
        Map<String, List<Long>> memberEvents = dataRepository.getMemberEventsOverTime(1);
        int joins = memberEvents.get("joins").size();
        int leaves = memberEvents.get("leaves").size();
        
        Map<Long, Long> channelData = dataRepository.getMostActiveChannels(1, 1);
        String mostActiveChannel = "N/A";
        long channelMessages = 0;
        if (!channelData.isEmpty()) {
            Long channelId = channelData.keySet().iterator().next();
            channelMessages = channelData.get(channelId);
            Channel channel = context.author().getGuild().getGuildChannelById(channelId);
            mostActiveChannel = channel != null ? "#" + channel.getName() : "Unknown";
        }
        
        Map<String, Long> reactionData = dataRepository.getTopReactions(1, 1);
        String mostUsedReaction = "N/A";
        long reactionCount = 0;
        if (!reactionData.isEmpty()) {
            mostUsedReaction = reactionData.keySet().iterator().next();
            reactionCount = reactionData.get(mostUsedReaction);
        }
        
        Map<Long, Long> allChannels = dataRepository.getMostActiveChannels(1, 1000);
        long totalMessages = allChannels.values().stream().mapToLong(Long::longValue).sum();
        
        int net = joins - leaves;
        String netSymbol = net >= 0 ? "+" : "-";
        
        String status = String.format("""
            ```diff
            Server Status
            -------------
            Membres:
            + Arrivées: %d
            - Départs: %d
            %s Net: %+d

            Messages:
              Total: %d
              Canal le plus actif: %s (%d messages)
            
            Réactions:
              Plus utilisée: %s (%d fois)
            ```
            """,
            joins,
            leaves,
            netSymbol,
            net,
            totalMessages,
            mostActiveChannel,
            channelMessages,
            mostUsedReaction,
            reactionCount
        );
        
        context.replyCallbackAction().setContent(status).queue();
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.status.description");
    }

    @NotNull
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
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("command.status.help");
    }

}
