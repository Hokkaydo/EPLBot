package com.github.hokkaydo.eplbot.module.tex;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TexModule extends Module {

    private final TexListener texListener;
    private final TexCommand texCommand;

    public TexModule(@NotNull Long guildId) {
        super(guildId);
        this.texListener = new TexListener(guildId);
        this.texCommand  = new TexCommand(guildId);
    }

    @Override
    public String getName() {
        return "tex";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(texCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(texListener, texCommand);
    }
}
