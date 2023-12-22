package com.github.hokkaydo.eplbot.module.earlybird;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EarlyBirdModule extends Module {

    private final EarlyBirdListener earlyBirdListener;
    private final EarlyBirdNextMessageCommand earlyBirdNextMessageCommand;
    public EarlyBirdModule(@NotNull Long guildId) {
        super(guildId);
        this.earlyBirdListener = new EarlyBirdListener(guildId);
        this.earlyBirdNextMessageCommand = new EarlyBirdNextMessageCommand(guildId, earlyBirdListener);
    }

    @Override
    public String getName() {
        return "earlybird";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(earlyBirdNextMessageCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(earlyBirdListener);
    }

    @Override
    public void enable() {
        super.enable();
        long rangeStartInSeconds = Config.getGuildVariable(getGuildId(), "EARLY_BIRD_RANGE_START_DAY_SECONDS");
        long rangeEndInSeconds = Config.getGuildVariable(getGuildId(), "EARLY_BIRD_RANGE_END_DAY_SECONDS");
        int messageProbability = Config.getGuildVariable(getGuildId(), "EARLY_BIRD_MESSAGE_PROBABILITY");
        long channelId = Config.getGuildVariable(getGuildId(), "EARLY_BIRD_CHANNEL_ID");
        earlyBirdListener.launchRandomSender(rangeStartInSeconds, rangeEndInSeconds, messageProbability, channelId);
    }

    @Override
    public void disable() {
        earlyBirdListener.cancel();
        super.disable();
    }

}
