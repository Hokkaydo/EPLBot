package com.github.hokkaydo.eplbot.module;

public abstract class Module {

    private boolean enabled = false;
    public abstract void registerCommand();
    public abstract void registerListener();
    public abstract String getName();
    public void disable() {
        this.enabled = false;
    }
    public void enable() {
        this.enabled = true;
    }
    public boolean isEnabled() {
        return this.enabled;
    }

}
