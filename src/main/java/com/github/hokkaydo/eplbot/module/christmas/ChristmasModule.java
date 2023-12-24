package com.github.hokkaydo.eplbot.module.christmas;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ChristmasModule extends Module {

    private final ChristmasListener christmasListener;
    protected ChristmasModule(@NotNull Long guildId) {
        super(guildId);
        this.christmasListener = new ChristmasListener(guildId);
    }

    @Override
    public String getName() {
        return "christmas";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(christmasListener);
    }

}
