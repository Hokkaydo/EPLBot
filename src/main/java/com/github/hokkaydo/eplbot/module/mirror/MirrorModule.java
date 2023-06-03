package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MirrorModule extends Module {


    private static final MirrorManager mirrorManager = new MirrorManager();
    private static int instanceCount = 0;
    private final MirrorLinkCommand mirrorLinkCommand;
    private final MirrorUnlinkCommand mirrorUnlinkCommand;
    private final MirrorListCommand mirrorListCommand;
    public MirrorModule(@NotNull Long guildId) {
        super(guildId);
        this.mirrorLinkCommand = new MirrorLinkCommand(mirrorManager);
        this.mirrorListCommand = new MirrorListCommand(mirrorManager);
        this.mirrorUnlinkCommand = new MirrorUnlinkCommand(mirrorManager);
        if(instanceCount == 0) {
            Main.getJDA().addEventListener(getListeners().toArray());
        }
    }

    @Override
    public void enable() {
        this.enabled = true;
        instanceCount++;
        Main.getCommandManager().enableCommands(getGuildId(), getCommandAsClass());
    }

    @Override
    public void disable() {
        this.enabled = false;
        instanceCount--;
        if(instanceCount == 0)
            Main.getJDA().removeEventListener(getListeners().toArray());
        Main.getCommandManager().disableCommands(getGuildId(), getCommandAsClass());
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

    public void loadMirrors() {
       mirrorManager.loadLinks();
    }

}
