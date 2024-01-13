package com.github.hokkaydo.eplbot.module.mirror.model;

import com.github.hokkaydo.eplbot.Main;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

public class MirrorLink {

    private final GuildMessageChannel first;
    private final GuildMessageChannel second;

    public MirrorLink(Long channelAId, Long channelBId ) {
        GuildChannel a = Main.getJDA().getGuildChannelById(channelAId);
        GuildChannel b = Main.getJDA().getGuildChannelById(channelBId);
        this.first = a instanceof GuildMessageChannel ga ? ga : null;
        this.second = b instanceof GuildMessageChannel gb ?  gb : null;
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
        return STR."MirrorLink{first=\{first}, second=\{second}\{'}'}";
    }

}
