package com.github.hokkaydo.eplbot.module.globalcommand;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import com.github.hokkaydo.eplbot.module.code.CodeCommand;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class GlobalCommandModule extends Module {

    private final EnableCommand enableCommand;
    private final DisableCommand disableCommand;
    private final ListFeaturesCommand listFeaturesCommand;
    private final ConfigurationCommand configurationCommand;
    private final RefreshCommandsCommand refreshCommandsCommand;
    private final StateCommand stateCommand;
    private final PingCommand pingCommand;
    private final HelpCommand helpCommand;
    private final ContributeCommand contributeCommand;
    private final IssueCommand issueCommand;
    private final CodeCommand codeCommand;

    public GlobalCommandModule(@NotNull Long guildId) {
        super(guildId);
        enableCommand = new EnableCommand(getGuildId());
        disableCommand = new DisableCommand(getGuildId());
        listFeaturesCommand = new ListFeaturesCommand(getGuildId());
        configurationCommand = new ConfigurationCommand(getGuildId());
        refreshCommandsCommand = new RefreshCommandsCommand();
        stateCommand = new StateCommand(guildId);
        pingCommand = new PingCommand();
        helpCommand = new HelpCommand(guildId);
        contributeCommand = new ContributeCommand();
        issueCommand = new IssueCommand();
        codeCommand = new CodeCommand();
    }

    @Override
    public String getName() {
        return "configuration";
    }

    @Override
    public List<Command> getCommands() {
        return Arrays.asList(
                enableCommand,
                disableCommand,
                listFeaturesCommand,
                configurationCommand,
                refreshCommandsCommand,
                stateCommand,
                pingCommand,
                helpCommand,
                contributeCommand,
                issueCommand,
                codeCommand
        );
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        List<ListenerAdapter> listeners = new ArrayList<>();
        listeners.add(issueCommand);
        listeners.add(codeCommand);
        return Arrays.asList(issueCommand);
    }
}
