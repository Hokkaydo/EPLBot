package com.github.hokkaydo.eplbot.module.rss;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class RssModule extends Module {

    private final RssReader reader;

    public RssModule(@NotNull Long guildId) {
        super(guildId);
        reader = new RssReader(guildId);
    }

    @Override
    public void enable() {
        super.enable();
        reader.launch();
    }

    @Override
    public void disable() {
        super.disable();
        reader.stop();
    }

    @Override
    public String getName() {
        return "rss";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
