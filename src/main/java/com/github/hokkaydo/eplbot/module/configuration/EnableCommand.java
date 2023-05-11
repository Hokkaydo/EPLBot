package com.github.hokkaydo.eplbot.module.configuration;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.function.Supplier;

public class EnableCommand implements Command {

    private final Long guildId;
    public EnableCommand(Long guildId) {
        this.guildId = guildId;
    }
    @Override
    public void executeCommand(CommandContext context) {
        OptionMapping featureOption = context.options().get(0);
        if(featureOption == null) return;
        Main.getModuleManager().enableGuildModule(featureOption.getAsString(), guildId);
        context.replyCallbackAction().setContent("Enabled `" + featureOption.getAsString() + "` :white_check_mark:").queue();
    }

    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_ENABLE_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "feature", Strings.getString("COMMAND_ENABLE_ARG_FEATURE_DESCRIPTION"), true)
                        .addChoices(Main.getModuleManager()
                                            .getGuildModules(guildId)
                                            .stream()
                                            .map(Module::getName)
                                            .map(n -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(n, n))
                                            .toList()
                        )
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
        return () -> Strings.getString("COMMAND_ENABLE_HELP");
    }
}
