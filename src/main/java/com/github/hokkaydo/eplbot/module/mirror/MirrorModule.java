package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MirrorModule extends Module {

    private final MirrorManager mirrorManager;
    private final MirrorLinkCommand mirrorLinkCommand;
    private final MirrorUnlinkCommand mirrorUnlinkCommand;
    private final MirrorListCommand mirrorListCommand;
    public MirrorModule(@NotNull Long guildId) {
        super(guildId);
        this.mirrorManager = new MirrorManager();
        this.mirrorLinkCommand = new MirrorLinkCommand(mirrorManager);
        this.mirrorListCommand = new MirrorListCommand(mirrorManager);
        this.mirrorUnlinkCommand = new MirrorUnlinkCommand(mirrorManager);
    }

    @Override
    public String getName() {
        return "mirror";
    }

    @Override
    public List<Command> getCommands() {
        return List.of(mirrorListCommand, mirrorLinkCommand, mirrorUnlinkCommand);
    }

    @Override
    public List<ListenerAdapter> getListeners() {
        return Collections.singletonList(mirrorManager);
    }

}
