package com.github.hokkaydo.eplbot.module.christmas;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.LocalDate;

public class ChristmasListener extends ListenerAdapter {

    private final Long guildId;
    ChristmasListener(Long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(LocalDate.now().getDayOfMonth() != 25 || LocalDate.now().getMonthValue() != 12) return;
        if(event.getAuthor().isBot()) return;
        if(event.getGuild().getIdLong() != guildId) return;
        if(event.getMessage().getContentRaw().toLowerCase().contains("joyeux") && event.getMessage().getContentRaw().toLowerCase().contains("noël")) {
            event.getChannel().sendMessage("Joyeux Noël ! :christmas_tree:").queue();
        }
        event.getMessage().addReaction(Emoji.fromUnicode("🎄")).queue();
    }

}
