package com.github.hokkaydo.eplbot.module.quote;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Pattern;

public class QuoteListener extends ListenerAdapter {

    private final Long guildId;
    QuoteListener(Long guildId) {
        this.guildId = guildId;
    }
    private static final Pattern MESSAGE_URL_PATTERN = Pattern.compile("https?://(canary.|ptb.)?discord.com/channels/\\d*/\\d*/\\d*");
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild()) return;
        if(event.getGuild().getIdLong() != guildId) return;
        event.getGuild()
                .loadMembers()
                .onSuccess(members ->
                                   MESSAGE_URL_PATTERN
                                           .matcher(event.getMessage().getContentDisplay())
                                           .results()
                                           .map(matchResult -> {
                                               String[] split = matchResult.group().split("/");
                                               return new Tuple3<>(split[4], split[5], split[6]);
                                           })
                                           .map(this::toMessage)
                                           .filter(Objects::nonNull)
                                           .forEach(m -> MessageUtil.toEmbedWithAttachements(
                                                           m,
                                                           e -> event.getMessage().replyEmbeds(
                                                                   e.setAuthor(
                                                                           MessageUtil.nameAndNickname(members, m.getAuthor()),
                                                                           m.getJumpUrl(),
                                                                           m.getAuthor().getAvatarUrl()
                                                                   ).build()
                                                           )
                                                   )
                                           )
                );
    }

    private record Tuple3<A, B, C>(A a, B b, C c) {}


    private Message toMessage(Tuple3<String, String, String> tuple3) {
        GuildChannel guildChannel = Main.getJDA().getGuildChannelById(tuple3.b);
        if(!(guildChannel instanceof MessageChannel)) return null;
        return ((MessageChannel)guildChannel).retrieveMessageById(tuple3.c).complete();
    }

}
