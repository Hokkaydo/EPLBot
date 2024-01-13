package com.github.hokkaydo.eplbot.module.quote;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class QuoteListener extends ListenerAdapter {

    private final Long guildId;
    QuoteListener(Long guildId) {
        this.guildId = guildId;
    }
    private static final Pattern MESSAGE_URL_PATTERN = Pattern.compile("https?://(canary.|ptb.)?discord.com/channels/\\d*/\\d*/\\d*");
    // key: quoter id, value: list of quotes quoted from quoter
    private final Map<Long, List<Message>> quotesOfMessage = new HashMap<>();
    // key: quote id, value: Quote
    private final Map<Long, Quote> quotes = new HashMap<>();

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if(event.getGuild() == null || event.getGuild().getIdLong() != guildId || event.getMember() == null) return; // not a guild event
        if(event.getButton().getId() == null || !event.getButton().getId().contains("delete-quote")) return;

        String modRoleId = Config.getGuildVariable(guildId,"MODERATOR_ROLE_ID");
        Role modRole = modRoleId.isBlank() ? null : Main.getJDA().getRoleById(Config.getGuildVariable(guildId,"MODERATOR_ROLE_ID"));
        if(modRole == null)
            MessageUtil.sendAdminMessage("Moderator role ID is no set !",guildId);
        boolean mod = event.getMember().getRoles().stream().max(Comparator.comparing(Role::getPosition)).map(role -> modRole != null && role.getPosition() >= modRole.getPosition()).orElse(false);
        if(!mod && (!quotes.containsKey(event.getMessageIdLong()) || !quotes.get(event.getMessageIdLong()).allowedToDeleteUserIds().contains(event.getUser().getIdLong()))) {
            event.getInteraction().deferReply(true).setContent(Strings.getString("QUOTE_DELETE_NOT_ALLOWED")).queue();
            return;
        }
        event.getInteraction().deferReply(true).setContent(Strings.getString("QUOTE_DELETED")).queue();
        event.getChannel().deleteMessageById(event.getMessageIdLong()).queue();
        quotes.remove(event.getMessageIdLong());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild()) return;
        if(event.getGuild().getIdLong() != guildId) return;
        event.getGuild().loadMembers().onSuccess(members -> quoteAndSend(members, event.getMessage()));
    }

    /**
     * Quote all messages referenced by links in given quoter
     * @param members members of current guild
     * @param quoter the message containing quoting links
     * */
    private void quoteAndSend(List<Member> members, Message quoter) {
        MESSAGE_URL_PATTERN
                .matcher(quoter.getContentDisplay())
                .results()
                .map(matchResult -> {
                    String[] split = matchResult.group().split("/");
                    return new Tuple3<>(split[4], split[5], split[6]);
                })
                .map(this::toMessage)
                .filter(Objects::nonNull)
                .forEach(quoted -> sendEmbed(members, quoter, quoted));
    }

    /**
     * Send quote embed
     * @param members guild members
     * @param quoter quoting message
     * @param quoted a message quoted in quoter
     * */
    private void sendEmbed(List<Member> members, Message quoter, Message quoted) {
        MessageUtil.toEmbedWithAttachements(
                quoted,
                e -> quoter.replyEmbeds(
                        e.setAuthor(
                                MessageUtil.nameAndNickname(
                                        members.stream().filter(member -> member.getIdLong() == quoted.getAuthor().getIdLong()).findFirst().orElse(null),
                                        quoted.getAuthor()
                                ),
                                quoted.getJumpUrl(),
                                quoted.getAuthor().getAvatarUrl()
                        ).build()
                ),
                quote -> {
                    List<Message> messages = quotesOfMessage.getOrDefault(quoter.getIdLong(), new ArrayList<>());
                    messages.add(quote);
                    quotesOfMessage.put(quote.getIdLong(), messages);
                    // Add 🗑 emote to delete quote if reacted with
                    quote.editMessageComponents(ActionRow.of(Button.primary("delete-quote", Emoji.fromUnicode("\uD83D\uDDD1")))).queue();
                    quotes.put(quote.getIdLong(), new Quote(quote, quoter.getAuthor().getIdLong(), quoted.getAuthor().getIdLong()));
                }
        );
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if(quotesOfMessage.containsKey(event.getMessageIdLong())) {
            // Deleting an already deleted message could throw an Exception
            quotesOfMessage.get(event.getMessageIdLong())
                    .stream()
                    .map(Message::delete).map(a -> a.onErrorMap(_ -> null))
                    .forEach(a -> {
                        try {
                            a.onErrorMap(_ -> null).queue();
                        }catch (Exception ignored) { /*Ignored*/}
                    });
            quotesOfMessage.remove(event.getMessageIdLong());
        }
    }

    private record Tuple3<A, B, C>(A a, B b, C c) {}


    private Message toMessage(Tuple3<String, String, String> tuple3) {
        GuildChannel guildChannel = Main.getJDA().getGuildChannelById(tuple3.b);
        if(!(guildChannel instanceof MessageChannel)) return null;
        return ((MessageChannel)guildChannel).retrieveMessageById(tuple3.c).complete();
    }

    private record Quote(Message quote, Long quoterUserId, Long quotedUserId){
        List<Long> allowedToDeleteUserIds() {
            return List.of(quotedUserId, quoterUserId);
        }
    }

}
