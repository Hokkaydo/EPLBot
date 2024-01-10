package com.github.hokkaydo.eplbot.module.christmas;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public class ChristmasListener extends ListenerAdapter {

    private final Long guildId;
    private static final Random RANDOM = new Random();
    private static final List<String> EMOTES = List.of(
            "🎄", "🎅", "🎁", "\uD83C\uDF1F", "\uD83C\uDF89", "☃ ️"
    );
    ChristmasListener(Long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(LocalDate.now().getDayOfMonth() != 25 || LocalDate.now().getMonthValue() != 12) return;
        if(event.getAuthor().isBot()) return;
        if(event.getGuild().getIdLong() != guildId) return;
        if(event.getMessage().getContentRaw().toLowerCase().contains("joyeux") && event.getMessage().getContentRaw().toLowerCase().contains("noël")) {
            event.getChannel().sendMessage("Joyeux Noël ! :christmas_tree:").queue();
        }
        if(RANDOM.nextInt(5) == 1) {
            event.getMessage().addReaction(Emoji.fromUnicode(EMOTES.get(RANDOM.nextInt(EMOTES.size())))).queue();
        }
    }

}
