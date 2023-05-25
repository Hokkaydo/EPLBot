package com.github.hokkaydo.eplbot.module.configuration;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigurationModule extends Module {

    private final EnableCommand enableCommand;
    private final DisableCommand disableCommand;
    private final ListFeaturesCommand listFeaturesCommand;
    private final ConfigurationCommand configurationCommand;
    private final RefreshCommand refreshCommand;

    public ConfigurationModule(@NotNull Long guildId) {
        super(guildId);
        enableCommand = new EnableCommand(getGuildId());
        disableCommand = new DisableCommand(getGuildId());
        listFeaturesCommand = new ListFeaturesCommand(getGuildId());
        configurationCommand = new ConfigurationCommand(getGuildId());
        refreshCommand = new RefreshCommand();
    }

    @Override
    public String getName() {
        return "configuration";
    }

    @Override
    public List<Command> getCommands() {
        return Arrays.asList(enableCommand, disableCommand, listFeaturesCommand, configurationCommand, refreshCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }
}
