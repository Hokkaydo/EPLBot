package com.github.hokkaydo.eplbot.module.autopin;

import com.github.hokkaydo.eplbot.Config;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class AutoPinListener extends ListenerAdapter {

    private final Long guildId;
    AutoPinListener(Long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(event.getGuild().getIdLong() != guildId) return;
        if(!event.getReaction().getEmoji().getAsReactionCode().equals(Config.<String>getGuildVariable(event.getGuild().getIdLong(), "PIN_REACTION_NAME"))) return;
        event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
            MessageReaction reaction = message.getReaction(Emoji.fromFormatted(event.getReaction().getEmoji().getFormatted()));
            if(reaction == null) return;
            if (reaction.getCount() >= Config.<Integer>getGuildVariable(event.getGuild().getIdLong(), "PIN_REACTION_THRESHOLD")) {
                event.getChannel().asTextChannel().pinMessageById(event.getMessageIdLong()).queue();
            }
        });
    }

}
