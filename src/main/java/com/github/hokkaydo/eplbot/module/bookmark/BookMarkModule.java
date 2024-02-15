package com.github.hokkaydo.eplbot.module.bookmark;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.bookmark.repository.BookMarkRepository;
import com.github.hokkaydo.eplbot.module.bookmark.repository.BookMarkRepositorySQLite;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BookMarkModule extends Module {

    private final BookMarkListener bookMarkListener;
    private final BookMarkCommand bookMarkCommand;
    public BookMarkModule(@NotNull Long guildId) {
        super(guildId);
        BookMarkRepository bookMarkRepository = new BookMarkRepositorySQLite(DatabaseManager.getDataSource());
        this.bookMarkListener = new BookMarkListener(guildId, bookMarkRepository);
        this.bookMarkCommand = new BookMarkCommand(bookMarkRepository);
    }

    @Override
    public String getName() {
        return "bookmark";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(bookMarkCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(bookMarkListener);
    }

}
