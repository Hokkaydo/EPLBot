package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class EnableCommand implements Command {

    private final Long guildId;
    EnableCommand(Long guildId) {
        this.guildId = guildId;
    }
    @Override
    public void executeCommand(CommandContext context) {
        OptionMapping featureOption = context.options().getFirst();
        if(featureOption == null) return;
        if(Main.getModuleManager().enableModule(featureOption.getAsString(), guildId)) {
            context.replyCallbackAction().setContent("Enabled `%s` :white_check_mark:".formatted(featureOption.getAsString())).queue();
        }
        else {
            context.replyCallbackAction().setContent("Failed to enable `%s` :x:. Maybe was it already enabled?".formatted(featureOption.getAsString())).queue();
        }
    }

    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.enable.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "feature", Strings.getString("command.enable.option.feature.description"), true)
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
        return false;
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
        return () -> Strings.getString("command.enable.help");
    }
}
