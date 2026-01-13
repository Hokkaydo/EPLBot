package com.github.hokkaydo.eplbot.module.data;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DataListener extends ListenerAdapter {

    private final long guildId;
    private final DataWriter dataWriter;

    public DataListener(Long guildId, DataWriter dataWriter) {
        this.guildId = guildId;
        this.dataWriter = dataWriter;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getGuild().getIdLong() != guildId) return;
        
        User author = event.getAuthor();
        if (author.isBot()) return;
        
        Channel channel = event.getChannel();

        dataWriter.logMessageReceived(author.getIdLong(), channel.getIdLong());
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getGuild().getIdLong() != guildId) return;
        
        User user = event.getUser();
        if (user == null || user.isBot()) return;
        
        Channel channel = event.getChannel();

        String emoji = event.getEmoji().getName();
        if (event.getEmoji().getType().equals(Emoji.Type.CUSTOM)) {
            emoji = ":%s:".formatted(emoji);
        }
        
        dataWriter.logReactionAdded(user.getIdLong(), channel.getIdLong(), emoji);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != guildId) return;
        
        User user = event.getUser();
        
        dataWriter.logMemberJoin(user.getIdLong());
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if (event.getGuild().getIdLong() != guildId) return;
        
        User user = event.getUser();
        
        dataWriter.logMemberRemove(user.getIdLong());
    }

}
