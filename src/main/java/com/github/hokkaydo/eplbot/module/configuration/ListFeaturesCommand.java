package com.github.hokkaydo.eplbot.module.configuration;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ListFeaturesCommand implements Command {

    private final Long guildId;
    public ListFeaturesCommand(Long guildId) {
        this.guildId = guildId;
    }
    @Override
    public void executeCommand(CommandContext context) {
        context.replyCallbackAction().setContent(
                Main.getModuleManager().getModules(guildId)
                        .stream()
                        .map(feature -> "`" + feature.getName() + "`: " + (feature.isEnabled() ? ":white_check_mark:" : ":x:"))
                        .reduce((s1, s2) -> s1 + "\n" + s2)
                        .orElse("")
        ).queue();
    }

    @Override
    public String getName() {
        return "listfeatures";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_LISTFEATURES_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
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
