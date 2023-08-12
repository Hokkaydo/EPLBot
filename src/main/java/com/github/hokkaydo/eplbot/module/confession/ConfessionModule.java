package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.confession.repository.WarnedConfessionRepository;
import com.github.hokkaydo.eplbot.module.confession.repository.WarnedConfessionRepositorySQLite;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
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
        DataSource dataSource = Main.getDataSource();
        this.processor = new ConfessionProcessor(guildId, lastConfession, new WarnedConfessionRepositorySQLite(dataSource));
        confessionCommand = new ConfessionCommand(processor);
        confessionFollowCommand = new ConfessionFollowCommand(lastConfession, processor);
        clearConfessWarningsCommand = new ClearConfessWarningsCommand(processor);
    }

    @Override
    public String getName() {
        return "confession";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(clearConfessWarningsCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(processor);
    }

    public List<Command> getGlobalCommands() {
        return List.of(confessionCommand, confessionFollowCommand);
    }

}
