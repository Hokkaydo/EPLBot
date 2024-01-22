package com.github.hokkaydo.eplbot.module.bookmark;

import com.github.hokkaydo.eplbot.module.bookmark.model.BookMark;
import com.github.hokkaydo.eplbot.module.bookmark.repository.BookMarkRepository;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class BookMarkListener extends ListenerAdapter {

    private static final String BOOKMARK_EMOJI = "🔖";
    private final long guildId;
    private final BookMarkRepository repository;
    BookMarkListener(Long guildId, BookMarkRepository bookMarkRepository) {
        this.guildId = guildId;
        this.repository = bookMarkRepository;
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if(event.getGuild().getIdLong() != guildId) return;

        repository.deleteByMessageId(event.getMessageIdLong());
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(event.getGuild().getIdLong() != guildId) return;
        if(!event.getEmoji().getAsReactionCode().equals(BOOKMARK_EMOJI)) return;

        event.retrieveMessage().queue(message -> {
            String content = message.getContentDisplay();
            if(content.length() > 100) {
                content = STR."\{content.substring(0, 100)}...";
            }
            repository.create(new BookMark(event.getUserIdLong(), event.getMessageIdLong(), content, message.getJumpUrl()));
        });

    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if(event.getGuild().getIdLong() != guildId) return;
        if(!event.getEmoji().getAsReactionCode().equals(BOOKMARK_EMOJI)) return;

        repository.deleteByUserAndMessageId(event.getUserIdLong(), event.getMessageIdLong());
    }

}
