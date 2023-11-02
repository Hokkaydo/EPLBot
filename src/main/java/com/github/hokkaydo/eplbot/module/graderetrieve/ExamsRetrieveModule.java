package com.github.hokkaydo.eplbot.module.graderetrieve;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ExamsRetrieveModule extends Module {

    private final SetupRetrieveChannelCommand setupRetrieveChannelCommand;
    private final ExamsRetrieveListener examsRetrieveListener;
    public ExamsRetrieveModule(@NotNull Long guildId) {
        super(guildId);
        this.examsRetrieveListener = new ExamsRetrieveListener(guildId);
        this.setupRetrieveChannelCommand = new SetupRetrieveChannelCommand(guildId, examsRetrieveListener);
    }

    @Override
    public String getName() {
        return "examsretrieve";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(setupRetrieveChannelCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(examsRetrieveListener);
    }

}
