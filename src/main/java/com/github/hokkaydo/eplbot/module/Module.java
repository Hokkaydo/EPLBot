package com.github.hokkaydo.eplbot.module;

public interface Module {

    void registerCommand();
    void registerListener();
    String getName();
    boolean disable();
    boolean enable();

}
