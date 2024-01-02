package com.github.hokkaydo.eplbot.module.mirror.model;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MirrorLink {

    private final TextChannel first;
    private final TextChannel second;

    public MirrorLink(Long channelAId, Long channelBId ) {
        this.first = Main.getJDA().getTextChannelById(channelAId);
        this.second = Main.getJDA().getTextChannelById(channelBId);
    }
    public TextChannel other(TextChannel channel) {
        return first.getIdLong()  == channel.getIdLong() ? second : first;
    }

    public boolean has(TextChannel channel) {
        return first.getIdLong() == channel.getIdLong() || second.getIdLong() == channel.getIdLong();
    }

    public TextChannel first() {
        return first;
    }

    public TextChannel second() {
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
