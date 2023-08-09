package com.github.hokkaydo.eplbot.module.mirror;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class MirrorListCommand implements Command {

    private final MirrorManager mirrorManager;

    MirrorListCommand(MirrorManager mirrorManager) {
        this.mirrorManager = mirrorManager;
    }

    @Override
    public void executeCommand(CommandContext context) {
        Optional<OptionMapping> channelAOption = context.options().stream().filter(o -> o.getName().equals("channel_a")).findFirst();
        GuildMessageChannel channelA;
        if(channelAOption.isEmpty()) {
            if(!context.channel().getType().isGuild() || !context.channel().getType().isMessage()) {
                context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRRORLIST_CHANNEL_GUILD_TEXT")).queue();
                return;
            }
            channelA = (GuildMessageChannel) Main.getJDA().getGuildChannelById(ChannelType.TEXT, context.channel().getIdLong());
            if(channelA == null) {
                context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRRORLIST_CHANNEL_GUILD_TEXT")).queue();
                return;
            }
        }else {
            channelA = Main.getJDA().getChannelById(GuildMessageChannel.class, channelAOption.get().getAsString());
            if(channelA == null) {
                context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRROR_INVALID_CHANNEL").formatted(channelAOption.get().getAsString())).queue();
                return;
            }
        }
        List<MirrorManager.Mirror> mirrors = mirrorManager.getLinks(channelA);
        if(mirrors.isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("COMMAND_MIRRORLIST_NO_MIRROR").formatted(channelA.getAsMention())).queue();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder("__Liste des liens existants__ :\n");
        for (MirrorManager.Mirror link : mirrors) {
            MessageChannel first = link.first();
            MessageChannel second = link.second();
            stringBuilder.append("\t");
            stringBuilder.append(first.getAsMention()).append(" ");
            stringBuilder.append("(").append(link.first().getIdLong()).append(")").append(" <-> ");
            stringBuilder.append(second.getAsMention()).append(" ");
            stringBuilder.append("(").append(link.second().getIdLong()).append(")").append("\n");
        }
        context.replyCallbackAction().setContent(stringBuilder.toString()).queue();
    }

    @Override
    public String getName() {
        return "mirrorlist";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_MIRRORLIST_DESCRIPTION");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(new OptionData(OptionType.STRING, "channel_a", Strings.getString("COMMAND_MIRRORLINK_OPTION_CHANNEL_A_DESCRIPTION"), false));
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
        return () -> Strings.getString("COMMAND_MIRRORLIST_HELP");
    }

}
