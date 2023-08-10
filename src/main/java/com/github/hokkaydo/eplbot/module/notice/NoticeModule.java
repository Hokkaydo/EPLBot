package com.github.hokkaydo.eplbot.module.notice;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoticeModule extends Module {

    private final NoticeCommand noticeCommand;
    public NoticeModule(@NotNull Long guildId) {
        super(guildId);
        this.noticeCommand = new NoticeCommand(listCourses());
    }

    private Map<String, List<String[]>> listCourses() {
        //TODO retrieve from database
        return new HashMap<>();
    }

    @Override
    public String getName() {
        return "notice";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(noticeCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return List.of(noticeCommand);
    }

}
