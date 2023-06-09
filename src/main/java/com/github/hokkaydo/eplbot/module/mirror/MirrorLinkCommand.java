package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class MirrorLinkCommand implements Command {

    private final MirrorManager mirrorManager;
    public MirrorLinkCommand(MirrorManager mirrorManager) {
        this.mirrorManager = mirrorManager;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> channelAOption = context.options().stream().filter(o -> o.getName().equals("channel_a")).findFirst();
        Optional<OptionMapping> channelBOption = context.options().stream().filter(o -> o.getName().equals("channel_b")).findFirst();
        if(channelAOption.isEmpty() || channelBOption.isEmpty()) return;
        GuildMessageChannel channelA = Main.getJDA().getChannelById(GuildMessageChannel.class, channelAOption.get().getAsString());
        GuildMessageChannel channelB = Main.getJDA().getChannelById(GuildMessageChannel.class, channelBOption.get().getAsString());
        if(channelA == null) {
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRROR_INVALID_CHANNEL").formatted(channelAOption.get().getAsString())).queue();
            return;
        }
        if(channelB == null) {
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRROR_INVALID_CHANNEL").formatted(channelBOption.get().getAsString())).queue();
            return;
        }
        if(mirrorManager.existsLink(channelA, channelB)) {
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRRORLINK_LINK_EXISTS").formatted(channelA.getAsMention(), channelB.getAsMention())).queue();
            return;
        }
        mirrorManager.createLink(channelA, channelB);
        context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRRORLINK_LINK_CREATED").formatted(channelA.getAsMention(), channelB.getAsMention())).queue();
    }

    @Override
    public String getName() {
        return "mirrorlink";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_MIRRORLINK_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "channel_a", Strings.getString("COMMAND_MIRRORLINK_OPTION_CHANNEL_A_DESCRIPTION"), true),
                new OptionData(OptionType.STRING, "channel_b", Strings.getString("COMMAND_MIRRORLINK_OPTION_CHANNEL_B_DESCRIPTION"), true)
        );
    }

    @Override
    public boolean ephemeralReply() {
        return false;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }

    @Override
    public boolean adminOnly() {
        return true;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("COMMAND_MIRRORLINK_HELP");
    }

}
