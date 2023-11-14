package com.github.hokkaydo.eplbot.module.mirror.model;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

public class MirrorLink {

    private final GuildMessageChannel first;
    private final GuildMessageChannel second;

    public MirrorLink(Long channelAId, Long channelBId ) {
        this.first = (GuildMessageChannel) Main.getJDA().getGuildChannelById(channelAId);
        this.second = (GuildMessageChannel) Main.getJDA().getGuildChannelById(channelBId);
    }
    public GuildMessageChannel other(GuildMessageChannel channel) {
        return first.getIdLong()  == channel.getIdLong() ? second : first;
    }

    public boolean has(GuildMessageChannel channel) {
        return first.getIdLong() == channel.getIdLong() || second.getIdLong() == channel.getIdLong();
    }

    public GuildMessageChannel first() {
        return first;
    }

    public GuildMessageChannel second() {
        return second;
    }

    @Override
    public String toString() {
        return "MirrorLink{" +
                       "first=" + first +
                       ", second=" + second +
                       '}';
    }

}
