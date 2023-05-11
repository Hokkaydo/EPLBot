package com.github.hokkaydo.eplbot.module.configuration;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.GuildModule;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigurationModule extends GuildModule {

    private final EnableCommand enableCommand;
    private final DisableCommand disableCommand;
    private final ListFeaturesCommand listFeaturesCommand;

    public ConfigurationModule(@NotNull Long guildId) {
        super(guildId);
        enableCommand = new EnableCommand(getGuildId());
        disableCommand = new DisableCommand(getGuildId());
        listFeaturesCommand = new ListFeaturesCommand(getGuildId());
    }

    @Override
    public String getName() {
        return "configuration";
    }

    @Override
    public List<Command> getCommands() {
        return Arrays.asList(enableCommand, disableCommand, listFeaturesCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }
}
