package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    protected boolean enabled = false;

    private final Long guildId;

    public Module(@NotNull Long guildId) {
        this.guildId = guildId;
    }

    public Long getGuildId() {
        return guildId;
    }

    public abstract String getName();
    public boolean isEnabled() {
        return this.enabled;
    }

    public abstract List<Command> getCommands();
    public abstract List<ListenerAdapter> getListeners();


    public Guild getGuild() {
        return Main.getJDA().getGuildById(guildId);
    }

    public void enable() {
        this.enabled = true;
        Main.getJDA().addEventListener(getListeners().toArray());
        Main.getCommandManager().enableCommands(getGuildId(), getCommandAsClass());
    }

    public void disable() {
        this.enabled = false;
        Main.getJDA().removeEventListener(getListeners().toArray());
        Main.getCommandManager().disableCommands(getGuildId(), getCommandAsClass());
    }

    protected List<Class<? extends Command>> getCommandAsClass() {
        List<Class<? extends Command>> list = new ArrayList<>();
        getCommands().forEach(c -> list.add(c.getClass()));
        return list;
    }

    @Override
    public String toString() {
        return STR."\{getName()};\{getGuild().getName()}";
    }

}
