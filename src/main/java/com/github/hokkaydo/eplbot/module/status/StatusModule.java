package com.github.hokkaydo.eplbot.module.status;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.data.DataRepository;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class StatusModule extends Module {

    private final StatusCommand statusCommand;

    public StatusModule(@NotNull Long guildId) {
        super(guildId);
        DataRepository dataRepository = new DataRepository(DatabaseManager.getDataSource());
        statusCommand = new StatusCommand(dataRepository);
    }

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(statusCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
