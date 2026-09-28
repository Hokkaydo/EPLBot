package com.github.hokkaydo.eplbot.module.contributions;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Announces new files dropped in the EPL Drive contributions folder, so that they don't pile up unnoticed.
 * <br>
 * Requires rclone with a configured OneDrive remote, see {@link DriveLister}.
 * */
public class ContributionsModule extends Module {

    private final ContributionsWatcher watcher;
    private final ContributionsCommand command;

    public ContributionsModule(@NotNull Long guildId) {
        super(guildId);
        this.watcher = new ContributionsWatcher(guildId);
        this.command = new ContributionsCommand(guildId);
    }

    @Override
    public void enable() {
        // Without the remote, nothing can be listed: stay disabled rather than fail at every check
        if (DriveLister.getRemote().isBlank()) {
            getLogger().warn("CONTRIBUTIONS_REMOTE environment variable is not set, module not enabled");
            MessageUtil.sendAdminMessage(Strings.getString("contributions.remote_not_set"), getGuildId());
            return;
        }
        super.enable();
        watcher.launch();
    }

    @Override
    public void disable() {
        super.disable();
        watcher.stop();
    }

    @Override
    public String getName() {
        return "contributions";
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(command);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.emptyList();
    }

}
