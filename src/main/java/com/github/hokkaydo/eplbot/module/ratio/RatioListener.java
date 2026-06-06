package com.github.hokkaydo.eplbot.module.ratio;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class RatioListener extends ListenerAdapter {

    private static final Long RALOF_ID = 192631146566123520L;
    private static final Long FEUR_REACTION_ID = 1305209638844891177L;
    
    private final Long guildId;
    
    public RatioListener(Long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getGuild().getIdLong() != guildId) return;
        if(event.getAuthor().getIdLong() == RALOF_ID) {
            event.getMessage().addReaction(Emoji.fromCustom("Feur", FEUR_REACTION_ID, false)).queue();
        }
    }

}
