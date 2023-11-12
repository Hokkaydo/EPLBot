package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.time.Instant;
import java.util.function.Function;
import java.util.logging.Level;

public class MessageUtil {

    public static EmbedBuilder toEmbed(Message message) {
        return new EmbedBuilder()
                       .setAuthor(message.getAuthor().getName(), message.getJumpUrl(), message.getAuthor().getAvatarUrl())
                       .appendDescription(message.getContentRaw())
                       .setTimestamp(message.getTimeCreated())
                       .setFooter(message.getGuild().getName() + " - #" + message.getChannel().getName(), message.getGuild().getIconUrl());
    }

    public static void toEmbedWithAttachements(Message message, Function<EmbedBuilder, MessageCreateAction> send) {
        MessageCreateAction action = send.apply(toEmbed(message)).addFiles();
        message.getAttachments().stream()
                .map(m -> new Tuple3<>(m.getFileName(), m.getProxy().download(), m.isSpoiler()))
                .map(tuple3 -> tuple3.b()
                                       .thenApply(i -> FileUpload.fromData(i, tuple3.a()))
                                       .thenApply(f -> Boolean.TRUE.equals(tuple3.c()) ? f.asSpoiler() : f)
                )
                .map(c -> c.thenAccept(action::addFiles))
                .reduce((a,b) -> {a.join(); return b;})
                .ifPresentOrElse(
                        c -> {
                            c.join();
                            action.queue();
                        },
                        action::queue);
    }

    public static EmbedBuilder toEmbed(String content) {
        return new EmbedBuilder()
                       .setAuthor(Main.getJDA().getSelfUser().getName(), "https://github.com/Hokkaydo/EPLBot", Main.getJDA().getSelfUser().getAvatarUrl())
                       .appendDescription(content)
                       .setTimestamp(Instant.now());
    }

    public static void sendWarning(String content, MessageChannel channel) {
        channel.sendMessageEmbeds(toEmbed(content).setColor(Color.YELLOW).build()).queue();
    }

    private MessageUtil() {}

    public static void sendAdminMessage(String message, Long guildId) {
        TextChannel adminChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId, "ADMIN_CHANNEL_ID"));
        if(adminChannel == null) {
            Main.LOGGER.log(Level.WARNING, "Invalid admin channel");
            return;
        }
        adminChannel.sendMessage(message).queue();
    }

    private record Tuple3<A, B, C>(A a, B b, C c) {}


}