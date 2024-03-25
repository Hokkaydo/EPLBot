package com.github.hokkaydo.eplbot.module.quote;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class QuoteModule extends Module {

    private final QuoteListener listener;
    public QuoteModule(@NotNull Long guildId) {
        super(guildId);
        listener = new QuoteListener(guildId);
    }

    @Override
    public String getName() {
        return "quote";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(listener);
    }

    public boolean isQuote(Long messageId) {
        return listener.isQuote(messageId);
    }

}
