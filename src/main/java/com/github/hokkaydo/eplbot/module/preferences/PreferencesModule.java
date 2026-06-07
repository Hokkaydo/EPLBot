package com.github.hokkaydo.eplbot.module.preferences;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PreferencesModule extends Module {

    private final PreferencesCommand command;

    public PreferencesModule(@NotNull Long guildId) {
        super(guildId);
        this.command = new PreferencesCommand(guildId);
    }

    @Override
    public String getName() {
        return "preferences";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(command);
    }
}
