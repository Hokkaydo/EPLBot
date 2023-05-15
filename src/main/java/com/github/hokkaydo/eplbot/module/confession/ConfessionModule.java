package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ConfessionModule extends Module {

    private final ConfessionCommand confessionCommand;
    public ConfessionModule(@NotNull Long guildId) {
        super(guildId);
        confessionCommand = new ConfessionCommand(guildId);
    }

    @Override
    public String getName() {
        return "confession";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(confessionCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(confessionCommand);
    }

    @Override
    public void enable() {
        Main.getJDA().addEventListener(getListeners().toArray());
        Main.getCommandManager().enableGlobalCommands(getCommands().stream().map(Command::getClass).toList());
    }

    @Override
    public void disable() {
        Main.getJDA().removeEventListener(getListeners().toArray());
        Main.getCommandManager().disableGlobalCommands(getCommands().stream().map(Command::getClass).toList());
    }

}
