package com.github.hokkaydo.eplbot.module.eplcommand;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class EPLCommandModule extends Module {

    private final DebugCommand debugCommand;
    private final FrameworkCommand frameworkCommand;

    public EPLCommandModule(@NotNull Long guildId) {
        super(guildId);
        debugCommand = new DebugCommand();
        frameworkCommand = new FrameworkCommand(getLogger());
    }

    @Override
    public String getName() {
        return "eplmodule";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(debugCommand, frameworkCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
