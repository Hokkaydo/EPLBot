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

public class DisableCommand implements Command {

    private final Long guildId;
    public DisableCommand(Long guildId) {
        this.guildId = guildId;
    }
    @Override
    public void executeCommand(CommandContext context) {
        OptionMapping featureOption = context.options().get(0);
        if(featureOption == null) return;
        Main.getModuleManager().disableModule(featureOption.getAsString(), guildId);
        context.replyCallbackAction().setContent("Disabled `" + featureOption.getAsString() + "` :x:").queue();
    }

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_DISABLE_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "feature", Strings.getString("COMMAND_DISABLE_ARG_FEATURE_DESCRIPTION"), true)
                        .addChoices(Main.getModuleManager()
                                            .getModules(guildId)
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
        return () -> Strings.getString("COMMAND_DISABLE_HELP");
    }
}
