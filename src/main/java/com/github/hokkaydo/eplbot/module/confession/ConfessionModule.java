package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfessionModule extends Module {

    private final ConfessionCommand confessionCommand;
    private final ConfessionFollowCommand confessionFollowCommand;
    private final ConfessionProcessor processor;
    private final ClearConfessWarningsCommand clearConfessWarningsCommand;
    public ConfessionModule(@NotNull Long guildId) {
        super(guildId);
        final Map<Long, Long> lastConfession = new HashMap<>();
        this.processor = new ConfessionProcessor(guildId, lastConfession);
        confessionCommand = new ConfessionCommand(processor);
        confessionFollowCommand = new ConfessionFollowCommand(lastConfession, processor);
        clearConfessWarningsCommand = new ClearConfessWarningsCommand(processor);
        Main.getCommandManager().addGlobalCommands(List.of(confessionCommand, confessionFollowCommand));
        Main.getCommandManager().addCommands(getGuild(), Collections.singletonList(clearConfessWarningsCommand));
    }

    @Override
    public String getName() {
        return "confession";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList(); // Workaround used to add some global & guild commands, need to rewrite
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(processor);
    }

    @Override
    public void enable() {
        this.enabled = true;
        Main.getJDA().addEventListener(getListeners().toArray());
        Main.getCommandManager().enableGlobalCommands(List.of(confessionCommand.getClass(), confessionFollowCommand.getClass()));
        Main.getCommandManager().enableCommands(getGuildId(), List.of(clearConfessWarningsCommand.getClass()));
    }

    @Override
    public void disable() {
        this.enabled = false;
        Main.getJDA().removeEventListener(getListeners().toArray());
        Main.getCommandManager().disableGlobalCommands(List.of(confessionCommand.getClass(), confessionFollowCommand.getClass()));
        Main.getCommandManager().disableCommands(getGuildId(), List.of(clearConfessWarningsCommand.getClass()));
    }

}
