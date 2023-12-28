package com.github.hokkaydo.eplbot.module.asciinema;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public class AsciinemaListener extends ListenerAdapter {

    private final Long guildId;
    AsciinemaListener(Long guildId) {
        this.guildId = guildId;
    }


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getContentDisplay().equals("ping")) {
            event.getChannel().sendMessage("pong !").queue();
        }
    }

}
