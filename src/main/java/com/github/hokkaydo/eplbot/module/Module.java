package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public abstract class Module {

    private boolean enabled = false;
    public abstract String getName();
    public void disable() {
        this.enabled = false;
        Main.getJDA().addEventListener(getListeners().toArray());
    }
    public void enable() {
        this.enabled = true;
        Main.getJDA().addEventListener(getListeners().toArray());
    }
    public boolean isEnabled() {
        return this.enabled;
    }

    public abstract List<Command> getCommands();
    public abstract List<ListenerAdapter> getListeners();

}
