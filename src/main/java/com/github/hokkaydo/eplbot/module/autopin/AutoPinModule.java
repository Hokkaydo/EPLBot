package com.github.hokkaydo.eplbot.module.autopin;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.GuildModule;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class AutoPinModule extends GuildModule {

    private final AutoPinListener listener;

    public AutoPinModule(@NotNull Guild guild) {
        super(guild);
        listener = new AutoPinListener(guild.getIdLong());
    }


    @Override
    public String getName() {
        return "autopin";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(listener);
    }

}
