package com.github.hokkaydo.eplbot.module;

import com.github.hokkaydo.eplbot.Main;

import java.util.ArrayList;
import java.util.List;

public abstract class GlobalModule extends Module {

    private final List<Long> jdaCommandIds = new ArrayList<>();

    @Override
    public void enable() {
        super.enable();
        jdaCommandIds.forEach(id -> Main.getCommandManager().removeCommand(id));
        jdaCommandIds.addAll(Main.getCommandManager().addGlobalCommand(getCommands()));
    }

    @Override
    public void disable() {
        super.disable();
        jdaCommandIds.forEach(id -> Main.getCommandManager().removeCommand(id));
    }

}
