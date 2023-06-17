package com.github.hokkaydo.eplbot.module.addcommand;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class AddCommandModule extends Module {

    private final AddCommandCommand addCommandCommand;
    private final AddCommandManager addCommandManager;

    public AddCommandModule(@NotNull Long guildId) {
        super(guildId);
        addCommandManager = new AddCommandManager(guildId);
        addCommandCommand = new AddCommandCommand(addCommandManager);
    }

    @Override
    public String getName() {
        return "addcommand";
    }

    @Override
    public List<Command> getCommands() {
        List<Command> commands = addCommandManager.getCommands();
        commands.add(addCommandCommand);
        System.out.println(commands);
        return commands;
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
