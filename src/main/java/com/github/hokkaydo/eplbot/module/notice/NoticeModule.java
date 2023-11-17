package com.github.hokkaydo.eplbot.module.notice;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.Module;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseGroupRepositorySQLite;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepository;
import com.github.hokkaydo.eplbot.module.graderetrieve.repository.CourseRepositorySQLite;
import com.github.hokkaydo.eplbot.module.notice.repository.NoticeRepository;
import com.github.hokkaydo.eplbot.module.notice.repository.NoticeRepositorySQLite;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoticeModule extends Module {

    private final NoticeCommand noticeCommand;
    public NoticeModule(@NotNull Long guildId) {
        super(guildId);
        CourseRepository courseRepository = new CourseRepositorySQLite(DatabaseManager.getDataSource());
        CourseGroupRepository groupRepository = new CourseGroupRepositorySQLite(DatabaseManager.getDataSource(), courseRepository);
        NoticeRepository noticeRepository = new NoticeRepositorySQLite(courseRepository, groupRepository);
        this.noticeCommand = new NoticeCommand(noticeRepository, courseRepository, groupRepository);
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
