package com.github.hokkaydo.eplbot.module.ratio;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class RatioModule extends Module {

    private final RatioListener ratioListener;
    public RatioModule(@NotNull Long guildId) {
        super(guildId);
        this.ratioListener = new RatioListener();
    }

    @Override
    public String getName() {
        return "ratio";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(ratioListener);
    }

}
