package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.Module;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    }

    @Override
    public synchronized void enable() {
        this.enabled = true;
        int current = instanceCount;
        if(current == 0) {
            Main.getJDA().addEventListener(getListeners().toArray());
        }
        instanceCount++;
        Main.getCommandManager().enableCommands(getGuildId(), getCommands());
    }

    @Override
    public synchronized void disable() {
        this.enabled = false;
        int current = instanceCount;
        if(current == 1)
            Main.getJDA().removeEventListener(getListeners().toArray());
        instanceCount--;
        Main.getCommandManager().disableCommands(getGuildId(), getCommands());
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

    static Map.Entry<GuildMessageChannel, GuildMessageChannel> validateChannels(CommandContext context) {
        Optional<OptionMapping> channelAOption = context.options().stream().filter(o -> o.getName().equals("channel_a")).findFirst();
        Optional<OptionMapping> channelBOption = context.options().stream().filter(o -> o.getName().equals("channel_b")).findFirst();
        if(channelAOption.isEmpty() || channelBOption.isEmpty()) return null;
        GuildMessageChannel channelA = Main.getJDA().getChannelById(GuildMessageChannel.class, channelAOption.get().getAsString());
        GuildMessageChannel channelB = Main.getJDA().getChannelById(GuildMessageChannel.class, channelBOption.get().getAsString());
        if(channelA == null) {
            context.replyCallbackAction().setContent(Strings.getString("command.mirror.invalid_channel_id").formatted(channelAOption.get().getAsString())).queue();
            return null;
        }
        if(channelB == null) {
            context.replyCallbackAction().setContent(Strings.getString("command.mirror.invalid_channel_id").formatted(channelBOption.get().getAsString())).queue();
            return null;
        }
        return Map.entry(channelA, channelB);
    }

}
