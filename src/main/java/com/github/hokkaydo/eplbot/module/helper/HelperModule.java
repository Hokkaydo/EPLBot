package com.github.hokkaydo.eplbot.module.helper;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.helper.repository.CourseHelperRepository;
import com.github.hokkaydo.eplbot.module.helper.repository.CourseHelperRepositorySQLite;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HelperModule extends Module {

    private final HelperCommand helperCommand;

    public HelperModule(@NotNull Long guildId) {
        super(guildId);
        CourseHelperRepository courseHelperRepository = new CourseHelperRepositorySQLite(DatabaseManager.getDataSource());
        this.helperCommand = new HelperCommand(guildId, courseHelperRepository);
    }

    @Override
    public String getName() {
        return "helper";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(helperCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(helperCommand);
    }

}
