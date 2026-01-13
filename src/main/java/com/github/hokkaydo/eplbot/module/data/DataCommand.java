package com.github.hokkaydo.eplbot.module.data;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class DataCommand implements Command {

    private static final String OPTION_TYPE = "type";
    private static final String OPTION_DURATION = "duration";
    private static final String TYPE_ACTIVE_CHANNELS = "active_channels";
    private static final String TYPE_TOP_REACTIONS = "top_reactions";
    private static final String TYPE_ACTIVE_USERS = "active_users";
    private static final String TYPE_ACTIVE_HOURS = "active_hours";
    private static final String TYPE_STATUS = "status";
    
    private final DataRepository repository;

    public DataCommand(DataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void executeCommand(CommandContext context) {
        String type = context.getOption(OPTION_TYPE)
            .map(OptionMapping::getAsString)
            .orElse(TYPE_ACTIVE_CHANNELS);
        
        int duration = context.getOption(OPTION_DURATION)
            .map(OptionMapping::getAsInt)
            .orElse(1);
        
        try {
            switch (type) {
                case TYPE_ACTIVE_CHANNELS -> generateActiveChannelsReport(context, duration);
                case TYPE_TOP_REACTIONS -> generateTopReactionsReport(context, duration);
                case TYPE_ACTIVE_USERS -> generateActiveUsersReport(context, duration);
                case TYPE_ACTIVE_HOURS -> generateActiveHoursReport(context, duration);
                case TYPE_STATUS -> generateStatusReport(context);
                default -> context.replyCallbackAction().setContent(Strings.getString("command.data.unknown_report_type")).queue();
            }
        } catch (IOException e) {
            context.replyCallbackAction().setContent(String.format(Strings.getString("command.data.error_generating_chart"), e.getMessage())).queue();
        }
    }

    private void generateActiveChannelsReport(CommandContext context, int durationDays) throws IOException {
        Set<DataRepository.ActiveChannel> channelData = repository.getMostActiveChannels(durationDays, 10);
        
        if (channelData.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_data")).queue();
            return;
        }
        
        Map<String, Long> namedChannelData = new LinkedHashMap<>();
        for (DataRepository.ActiveChannel activeChannel : channelData) {
            Channel channel = context.author().getGuild().getGuildChannelById(activeChannel.channelId());
            String channelName = channel != null ? channel.getName() : "Unknown (" + activeChannel.channelId() + ")";
            namedChannelData.put(channelName, activeChannel.messageCount());
        }
        
        byte[] chartImage = DataGrapher.generateChannelActivityChart(
            namedChannelData, 
            String.format(Strings.getString("command.data.chart_title.active_channels"), durationDays)
        );
        
        context.replyCallbackAction()
            .setContent(String.format(Strings.getString("command.data.chart_title.active_channels"), durationDays))
            .addFiles(FileUpload.fromData(chartImage, "active_channels.png"))
            .queue();
    }

    private void generateTopReactionsReport(CommandContext context, int durationDays) throws IOException {
        Set<DataRepository.ReactionCount> reactionData = repository.getTopReactions(durationDays, 10);
        
        if (reactionData.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_reaction_data")).queue();
            return;
        }
        
        StringBuilder summary = new StringBuilder(String.format(Strings.getString("command.data.top_reactions_title"), durationDays) + "\n\n");
        int rank = 1;
        for (DataRepository.ReactionCount reactionCount : reactionData) {
            summary.append(String.format("%d. %s - %d uses\n", rank++, reactionCount.reactionEmoji(), reactionCount.count()));
        }
        
        context.replyCallbackAction()
            .setContent(summary.toString())
            .queue();
    }

    private void generateActiveUsersReport(CommandContext context, int durationDays) throws IOException {
        Set<DataRepository.ActiveUser> userData = repository.getMostActiveUsers(durationDays, 10);
        
        if (userData.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_data")).queue();
            return;
        }
        
        Map<String, Long> namedUserData = new LinkedHashMap<>();
        for (DataRepository.ActiveUser activeUser: userData) {
            User user = context.author().getJDA().getUserById(activeUser.userId());
            String userName = user != null ? user.getName() : "Unknown (" + activeUser.userId() + ")";
            namedUserData.put(userName, activeUser.messageCount());
        }
        
        byte[] chartImage = DataGrapher.generateUserActivityChart(
            namedUserData, 
            String.format(Strings.getString("command.data.chart_title.active_users"), durationDays)
        );
        
        context.replyCallbackAction()
            .setContent(String.format(Strings.getString("command.data.chart_title.active_users"), durationDays))
            .addFiles(FileUpload.fromData(chartImage, "active_users.png"))
            .queue();
    }

    private void generateActiveHoursReport(CommandContext context, int durationDays) throws IOException {
        Set<DataRepository.ActiveHour> hourData = repository.getMostActiveHours(durationDays);
        
        if (hourData.stream().allMatch(h -> h.messageCount() == 0)) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_data")).queue();
            return;
        }
        
        Map<String, Long> namedHourData = new LinkedHashMap<>();
        for (DataRepository.ActiveHour activeHour : hourData) {
            namedHourData.put(String.format("%02d:00", activeHour.hour()), activeHour.messageCount());
        }
        
        byte[] chartImage = DataGrapher.generateHourlyActivityChart(
            namedHourData, 
            String.format(Strings.getString("command.data.chart_title.active_hours"), durationDays)
        );
        
        context.replyCallbackAction()
            .setContent(String.format(Strings.getString("command.data.chart_title.active_hours"), durationDays))
            .addFiles(FileUpload.fromData(chartImage, "active_hours.png"))
            .queue();
    }

    private void generateStatusReport(CommandContext context) {
        Map<String, List<Long>> memberEvents = repository.getMemberEventsOverTime(1);
        int joins = memberEvents.get("joins").size();
        int leaves = memberEvents.get("leaves").size();
        
        Set<DataRepository.ActiveChannel> channelData = repository.getMostActiveChannels(1, 1);
        String mostActiveChannel = "N/A";
        long channelMessages = 0;
        if (!channelData.isEmpty()) {
            DataRepository.ActiveChannel activeChannel = channelData.stream().findFirst().orElseThrow();
            channelMessages = activeChannel.messageCount();
            Channel channel = context.author().getGuild().getGuildChannelById(activeChannel.channelId());
            mostActiveChannel = channel != null ? "#" + channel.getName() : "Unknown";
        }
        
        Set<DataRepository.ReactionCount> reactionData = repository.getTopReactions(1, 1);
        String mostUsedReaction = "N/A";
        long reactionCount = 0;
        if (!reactionData.isEmpty()) {
            DataRepository.ReactionCount reaction = reactionData.stream().findFirst().orElseThrow();
            mostUsedReaction = reaction.reactionEmoji();
            reactionCount = reaction.count();
        }
        
        Set<DataRepository.ActiveChannel> allChannels = repository.getMostActiveChannels(1, 1000);
        long totalMessages = allChannels.stream().mapToLong(DataRepository.ActiveChannel::messageCount).sum();
        
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
        return "data";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.data.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        
        OptionData typeOption = new OptionData(OptionType.STRING, OPTION_TYPE, Strings.getString("command.data.option.type.description"), true)
            .addChoice(Strings.getString("command.data.option.type.choice.active_channels"), TYPE_ACTIVE_CHANNELS)
            .addChoice(Strings.getString("command.data.option.type.choice.top_reactions"), TYPE_TOP_REACTIONS)
            .addChoice(Strings.getString("command.data.option.type.choice.active_users"), TYPE_ACTIVE_USERS)
            .addChoice(Strings.getString("command.data.option.type.choice.active_hours"), TYPE_ACTIVE_HOURS)
            .addChoice(Strings.getString("command.data.option.type.choice.status"), TYPE_STATUS);
        
        OptionData durationOption = new OptionData(OptionType.INTEGER, OPTION_DURATION, Strings.getString("command.data.option.duration.description"), false)
            .setMinValue(1)
            .setMaxValue(60); // Max 30 days
        
        options.add(typeOption);
        options.add(durationOption);
        
        return options;
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
        return () -> Strings.getString("command.data.help");
    }

}

