package com.github.hokkaydo.eplbot.module.data;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
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
import java.util.function.Supplier;

public class DataCommand implements Command {

    private static final String OPTION_TYPE = "type";
    private static final String OPTION_DURATION = "duration";
    private static final String TYPE_ACTIVE_CHANNELS = "active_channels";
    private static final String TYPE_MEMBER_EVENTS = "member_events";
    private static final String TYPE_TOP_REACTIONS = "top_reactions";
    private static final String TYPE_ACTIVE_USERS = "active_users";
    
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
                case TYPE_MEMBER_EVENTS -> generateMemberEventsReport(context, duration);
                case TYPE_TOP_REACTIONS -> generateTopReactionsReport(context, duration);
                case TYPE_ACTIVE_USERS -> generateActiveUsersReport(context, duration);
                default -> context.replyCallbackAction().setContent(Strings.getString("command.data.unknown_report_type")).queue();
            }
        } catch (IOException e) {
            context.replyCallbackAction().setContent(String.format(Strings.getString("command.data.error_generating_chart"), e.getMessage())).queue();
        }
    }

    private void generateActiveChannelsReport(CommandContext context, int durationDays) throws IOException {
        Map<Long, Long> channelData = repository.getMostActiveChannels(durationDays, 10);
        
        if (channelData.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_data")).queue();
            return;
        }
        
        Map<String, Long> namedChannelData = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : channelData.entrySet()) {
            Channel channel = context.author().getGuild().getGuildChannelById(entry.getKey());
            String channelName = channel != null ? channel.getName() : "Unknown (" + entry.getKey() + ")";
            namedChannelData.put(channelName, entry.getValue());
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

    private void generateMemberEventsReport(CommandContext context, int durationDays) throws IOException {
        Map<String, List<Long>> memberEvents = repository.getMemberEventsOverTime(durationDays);
        List<Long> joins = memberEvents.get("joins");
        List<Long> leaves = memberEvents.get("leaves");
        
        if (joins.isEmpty() && leaves.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_member_events")).queue();
            return;
        }
        
        byte[] chartImage = DataGrapher.generateMemberEventsChart(
            joins, 
            leaves, 
            String.format(Strings.getString("command.data.chart_title.member_events"), durationDays)
        );
        
        String summary = String.format(
            Strings.getString("command.data.member_activity_summary"),
            durationDays, joins.size(), leaves.size(), joins.size() - leaves.size()
        );
        
        context.replyCallbackAction()
            .setContent(summary)
            .addFiles(FileUpload.fromData(chartImage, "member_events.png"))
            .queue();
    }

    private void generateTopReactionsReport(CommandContext context, int durationDays) throws IOException {
        Map<String, Long> reactionData = repository.getTopReactions(durationDays, 10);
        
        if (reactionData.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_reaction_data")).queue();
            return;
        }
        
        StringBuilder summary = new StringBuilder(String.format(Strings.getString("command.data.top_reactions_title"), durationDays) + "\n\n");
        int rank = 1;
        for (Map.Entry<String, Long> entry : reactionData.entrySet()) {
            summary.append(String.format("%d. %s - %d uses\n", rank++, entry.getKey(), entry.getValue()));
        }
        
        context.replyCallbackAction()
            .setContent(summary.toString())
            .queue();
    }

    private void generateActiveUsersReport(CommandContext context, int durationDays) throws IOException {
        Map<Long, Long> userData = repository.getMostActiveUsers(durationDays, 10);
        
        if (userData.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.data.no_data")).queue();
            return;
        }
        
        Map<String, Long> namedUserData = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : userData.entrySet()) {
            net.dv8tion.jda.api.entities.User user = context.author().getJDA().getUserById(entry.getKey());
            String userName = user != null ? user.getName() : "Unknown (" + entry.getKey() + ")";
            namedUserData.put(userName, entry.getValue());
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
            .addChoice(Strings.getString("command.data.option.type.choice.member_events"), TYPE_MEMBER_EVENTS)
            .addChoice(Strings.getString("command.data.option.type.choice.top_reactions"), TYPE_TOP_REACTIONS)
            .addChoice(Strings.getString("command.data.option.type.choice.active_users"), TYPE_ACTIVE_USERS);
        
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

