package com.github.hokkaydo.eplbot.module.basic;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BasicCommandModule extends Module {

    private final ClearBetween clearBetweenCommand;
    private final ClearFromCommand clearFromCommand;
    private final ClearLastCommand clearLastCommand;
    private final HelpCommand helpCommand;
    private final MoveMessagesCommand moveMessagesCommand;
    public BasicCommandModule(@NotNull Long guildId) {
        super(guildId);
        clearFromCommand = new ClearFromCommand();
        clearBetweenCommand = new ClearBetween();
        clearLastCommand = new ClearLastCommand();
        helpCommand = new HelpCommand(guildId);
        moveMessagesCommand = new MoveMessagesCommand();
    }

    @Override
    public String getName() {
        return "basiccommands";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(clearBetweenCommand, clearFromCommand, clearLastCommand, helpCommand, moveMessagesCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
