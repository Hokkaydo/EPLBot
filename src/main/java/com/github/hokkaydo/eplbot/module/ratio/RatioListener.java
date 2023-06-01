package com.github.hokkaydo.eplbot.module.ratio;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class RatioListener extends ListenerAdapter {

    private static final Long FUCK_THEM_MESSAGE_ID = 1113769154684653569L;
    private static final Random random = new Random();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        GuildChannel channel = Main.getJDA().getGuildChannelById(517720163223601155L);
        if (channel == null) return;
        TextChannel textChannel = (TextChannel) channel;
        textChannel.retrieveMessageById(FUCK_THEM_MESSAGE_ID).queue(m -> m.getReactions().stream().filter(r -> r.getEmoji().getName().equals("epl_Smart")).findFirst().map(MessageReaction::retrieveUsers).ifPresent(a -> a.queue(l -> {
            if(l.stream().anyMatch(u -> u.getIdLong() == event.getAuthor().getIdLong()) && random.nextInt(100) < 10) {
                event.getMessage().addReaction(Emoji.fromCustom("ratio", 1113815294541041674L, false)).queue();
            }
        })));
    }

}
