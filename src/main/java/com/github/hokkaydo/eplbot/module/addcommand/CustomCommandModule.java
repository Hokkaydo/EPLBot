package com.github.hokkaydo.eplbot.module.addcommand;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CustomCommandModule extends Module {

    private final AddCommandCommand addCommandCommand;
    private final CustomCommandManager customCommandManager;

    public CustomCommandModule(@NotNull Long guildId) {
        super(guildId);
        customCommandManager = new CustomCommandManager(guildId);
        addCommandCommand = new AddCommandCommand(customCommandManager);
    }

    @Override
    public String getName() {
        return "customcommands";
    }

    @Override
    public List<Command> getCommands() {
        List<Command> commands = customCommandManager.getCommands();
        commands.add(addCommandCommand);
        return commands;
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
