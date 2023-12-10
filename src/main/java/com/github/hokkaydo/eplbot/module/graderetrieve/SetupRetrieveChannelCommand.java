package com.github.hokkaydo.eplbot.module.graderetrieve;

import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SetupRetrieveChannelCommand implements Command {

    private final Long guildId;
    private final ExamsRetrieveListener examsRetrieveListener;
    SetupRetrieveChannelCommand(Long guildId, ExamsRetrieveListener examsRetrieveListener) {
        this.guildId = guildId;
        this.examsRetrieveListener = examsRetrieveListener;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> quarterOpt = context.options().stream().filter(e -> e.getName().equals("quarter")).findFirst();
        if(quarterOpt.isEmpty()) return;
        examsRetrieveListener.setGradeRetrieveChannelId(context.channel().getIdLong(), quarterOpt.get().getAsInt());
        Config.updateValue(guildId, "EXAM_RETRIEVE_CHANNEL", quarterOpt.get().getAsInt());
    }

    @Override
    public String getName() {
        return "setupexamsretrievechannel";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_SETUPGRADECHANNEL_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.INTEGER,"quarter", "Quadrimestre", true).addChoice("1", 1).addChoice("2", 2));
    }

    @Override
    public boolean ephemeralReply() {
        return true;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return !(channel instanceof PrivateChannel);
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_SETUPGRADECHANNEL_HELP");
    }

}
