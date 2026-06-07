package com.github.hokkaydo.eplbot.module.tex;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class TexModule extends Module {

    private final TexListener texListener;

    public TexModule(@NotNull Long guildId) {
        super(guildId);
        this.texListener = new TexListener(guildId);
    }

    @Override
    public String getName() {
        return "tex";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(texListener);
    }
}
