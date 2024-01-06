package com.github.hokkaydo.eplbot;

import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class MessageUtil {

    private static final String HASTEBIN_API_POST_URL = "https://hastebin.com/documents/";
    private static final String HASTEBIN_SHARE_BASE_URL = "https://hastebin.com/share/%s";
    private static final int HASTEBIN_MAX_CONTENT_LENGTH = 350_000;

    private static HttpRequest.Builder hastebinPostRequest = null;


    public static EmbedBuilder toEmbed(Message message) {
        return new EmbedBuilder()
                       .setAuthor(message.getAuthor().getName(), message.getJumpUrl(), message.getAuthor().getAvatarUrl())
                       .appendDescription(message.getContentRaw())
                       .setTimestamp(message.getTimeCreated())
                       .setFooter(message.getGuild().getName() + " - #" + message.getChannel().getName(), message.getGuild().getIconUrl());
    }

    public static void toEmbedWithAttachements(Message message, Function<EmbedBuilder, MessageCreateAction> send, Consumer<Message> processSentMessage) {
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
                            action.queue(processSentMessage);
                        },
                        () -> action.queue(processSentMessage)
                );
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

    public static String nameAndNickname(@Nullable Member member, User user) {
        boolean hasNickname = member != null && member.getNickname() != null;
        return (hasNickname  ? member.getNickname() + " (" : "") + user.getEffectiveName() + (hasNickname ? ")" : "");
    }

    /**
     * Post content on Hastebin and return shareable link
     * @param client an {@link HttpClient} initialized by the caller to make multiple calls on the same client
     * @param data the content to post
     * @return a {@link CompletableFuture} returning the shareable link or an empty string if an error arise
     * */
    public static CompletableFuture<String> hastebinPost(HttpClient client, String data) {
        if(data.length() > HASTEBIN_MAX_CONTENT_LENGTH) throw new IllegalArgumentException("'data' should be shorted than %d".formatted(HASTEBIN_MAX_CONTENT_LENGTH));
        if (hastebinPostRequest == null)
            hastebinPostRequest = HttpRequest.newBuilder()
                                          .header("Content-Type", "text/plain")
                                          .header("Authorization", "Bearer " + System.getenv("HASTEBIN_API_TOKEN"))
                                          .uri(URI.create(HASTEBIN_API_POST_URL));

        return client.sendAsync(hastebinPostRequest.POST(HttpRequest.BodyPublishers.ofString(data)).build(), HttpResponse.BodyHandlers.ofString())
                       .thenApply(HttpResponse::body)
                       .thenApply(JSONObject::new)
                       .thenApply(response -> {
                           if (!response.has("key")) {
                               return "";
                           }
                           return HASTEBIN_SHARE_BASE_URL.formatted(response.get("key"));
                       });
    }
    private record Tuple3<A, B, C>(A a, B b, C c) {}


}