package com.github.hokkaydo.eplbot.module.eplcommand;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class EPLCommandModule extends Module {

    private final ClearBetween clearBetweenCommand;
    private final ClearFromCommand clearFromCommand;
    private final ClearLastCommand clearLastCommand;
    private final MoveMessagesCommand moveMessagesCommand;
    public EPLCommandModule(@NotNull Long guildId) {
        super(guildId);
        clearFromCommand = new ClearFromCommand();
        clearBetweenCommand = new ClearBetween();
        clearLastCommand = new ClearLastCommand();
        moveMessagesCommand = new MoveMessagesCommand();
    }

    @Override
    public String getName() {
        return "basiccommands";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(clearBetweenCommand, clearFromCommand, clearLastCommand, moveMessagesCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
