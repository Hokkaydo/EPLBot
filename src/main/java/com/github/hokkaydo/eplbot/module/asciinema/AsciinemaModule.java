package com.github.hokkaydo.eplbot.module.asciinema;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AsciinemaModule extends Module {


    private final AsciinemaListener asciinemaListener;
    public AsciinemaModule(@NotNull Long guildId) {
        super(guildId);
        this.asciinemaListener = new AsciinemaListener();
    }

    @Override
    public String getName() {
        return "asciinema";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(asciinemaListener);
    }

}
