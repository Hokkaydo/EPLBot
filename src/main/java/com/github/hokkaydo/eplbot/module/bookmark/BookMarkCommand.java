package com.github.hokkaydo.eplbot.module.bookmark;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.bookmark.model.BookMark;
import com.github.hokkaydo.eplbot.module.bookmark.repository.BookMarkRepository;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class BookMarkCommand implements Command {

    private final BookMarkRepository repository;
    BookMarkCommand(BookMarkRepository repository) {
        this.repository = repository;
    }

    @Override
    public void executeCommand(CommandContext context) {
        List<BookMark> bookMarks = repository.getByUserId(context.user().getIdLong());
        if(bookMarks.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.bookmark.empty")).queue();
            return;
        }
        context.replyCallbackAction().setContent(Strings.getString("check_your_dms")).queue();
        context.user().openPrivateChannel().queue(channel -> {
            StringBuilder message = new StringBuilder("__Liste de vos signets :__");
            for (BookMark bookMark : bookMarks) {
                if(message.length() > Message.MAX_CONTENT_LENGTH) {
                    channel.sendMessage(message).queue();
                    message = new StringBuilder();
                }
                message.append("\n").append(this.formatter(bookMark));
            }
            channel.sendMessage(message).queue();
        });
    }

    private String formatter(BookMark bookMark) {
        String content = bookMark.description();
        if (content.length() > 100) {
            content = content.substring(0, 100) + "...";
        }
        return """
        > %s
        %s
        """.formatted(content, bookMark.messageLink());
    }

    @Override
    public String getName() {
        return "bookmark";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.bookmark.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public boolean ephemeralReply() {
        return true;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }

    @Override
    public boolean adminOnly() {
        return false;
    }

    @Override
    public Supplier<String> help() {
            return () -> Strings.getString("command.bookmark.help");
    }

}
