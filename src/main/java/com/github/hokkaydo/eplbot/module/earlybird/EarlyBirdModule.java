package com.github.hokkaydo.eplbot.module.earlybird;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EarlyBirdModule extends Module {

    private final EarlyBirdListener earlyBirdListener;
    private final EarlyBirdNextMessageCommand earlyBirdNextMessageCommand;
    private final RestartEarlyBirdCommand restartEarlyBirdCommand;
    public EarlyBirdModule(@NotNull Long guildId) {
        super(guildId);
        this.earlyBirdListener = new EarlyBirdListener(guildId);
        this.earlyBirdNextMessageCommand = new EarlyBirdNextMessageCommand();
        this.restartEarlyBirdCommand = new RestartEarlyBirdCommand(earlyBirdListener);
    }

    @Override
    public String getName() {
        return "earlybird";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(earlyBirdNextMessageCommand, restartEarlyBirdCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(earlyBirdListener);
    }

    @Override
    public void enable() {
        super.enable();
        earlyBirdListener.launchRandomSender();
    }

    @Override
    public void disable() {
        earlyBirdListener.cancel();
        super.disable();
    }

}
