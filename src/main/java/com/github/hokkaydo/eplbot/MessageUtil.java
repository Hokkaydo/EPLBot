package com.github.hokkaydo.eplbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MessageUtil {

    public static EmbedBuilder toEmbed(Message message) {
        return new EmbedBuilder()
                       .setAuthor(message.getAuthor().getAsTag(), message.getJumpUrl(), message.getAuthor().getAvatarUrl())
                       .appendDescription(message.getContentDisplay())
                       .setTimestamp(message.getTimeCreated())
                       .setFooter(message.getGuild().getName() + " - #" + message.getChannel().getName(), Main.getJDA().getSelfUser().getAvatarUrl());
    }

    private MessageUtil() {}

    public static void sendAdminMessage(String message, Long guildId) {
        TextChannel adminChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildValue(guildId, "ADMIN_CHANNEL_ID"));
        if(adminChannel == null) {
            System.err.println("[WARNING] Invalid admin channel");
            return;
        }
        adminChannel.sendMessage(message).queue();
    }

}