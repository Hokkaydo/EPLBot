package com.github.hokkaydo.eplbot.module.confession;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.Config;
import com.github.hokkaydo.eplbot.module.confession.model.WarnedConfession;
import com.github.hokkaydo.eplbot.module.confession.repository.WarnedConfessionRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.Color;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ConfessionProcessor extends ListenerAdapter {

    private final Map<UUID, MessageCreateBuilder> confessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> confessionsContent = new ConcurrentHashMap<>();
    private final List<UUID> confessFollowing = new ArrayList<>();
    private final Map<UUID, Long> confessionAuthor = new ConcurrentHashMap<>();
    private static final String[] VALIDATION_EMBED_TITLES = {"Confession - Validée", "Confession - Refusée", "Confession - Signalée"};
    private static final Color[] VALIDATION_EMBED_COLORS = {Color.GREEN, Color.RED, Color.YELLOW};
    private static final int VALID = 0;
    private static final int REFUSED = 1;
    private static final int WARNED = 2;

    private static final String CONFESSION = "confession";

    private final Long guildId;
    private final Map<Long, Long> lastMainConfession;
    private final Map<Long, Long> lastMainConfessionValidation;
    private final Map<Long, Long> lastFollowingConfessionValidation;
    private final WarnedConfessionRepository warnedConfessionRepository;

    ConfessionProcessor(Long guildId, Map<Long, Long> lastMainConfession, WarnedConfessionRepository warnedConfessionRepository) {
        this.guildId = guildId;
        this.lastMainConfession = lastMainConfession;
        this.lastFollowingConfessionValidation = new ConcurrentHashMap<>();
        this.lastMainConfessionValidation = new ConcurrentHashMap<>();
        this.warnedConfessionRepository = warnedConfessionRepository;
    }

    void process(CommandContext context, boolean following) {
        String confessionValidationChannelId = Config.getGuildVariable(guildId, "CONFESSION_VALIDATION_CHANNEL_ID");
        if(confessionValidationChannelId.isBlank()) {
            MessageUtil.sendAdminMessage(Strings.getString("command.confession.validation_channel_id_invalid"), guildId);
            context.replyCallbackAction().setContent(Strings.getString("error_occurred")).queue();
            return;
        }
        TextChannel validationChannel = Main.getJDA().getChannelById(TextChannel.class, confessionValidationChannelId);
        if(validationChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("command.confession.validation_channel_id_invalid"), guildId);
            context.replyCallbackAction().setContent(Strings.getString("error_occurred")).queue();
            return;
        }
        TextInput.Builder textBuilder = TextInput.create(CONFESSION, TextInputStyle.PARAGRAPH)
                                                .setPlaceholder("Confessez-vous ici");
        Modal confession = Modal.create(CONFESSION, "Confession")
                                   .addComponents(Label.of("Confession", textBuilder.build()))
                                   .build();
        context.interaction().replyModal(confession).queue();

        Main.getJDA().listenOnce(ModalInteractionEvent.class)
                .filter(e -> e.getUser().getIdLong() == context.user().getIdLong())
                .filter(e -> e.getModalId().equals(CONFESSION))
                .subscribe(e -> processConfession(Objects.requireNonNull(e.getValue(CONFESSION)).getAsString(), e, following, validationChannel));
    }

    private void processConfession(String confession, ModalInteractionEvent event, boolean following, TextChannel validationChannel) {
        long userId = event.getUser().getIdLong();
        if(confession.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
            event.reply(Strings.getString("command.confession.too_long").formatted(MessageEmbed.DESCRIPTION_MAX_LENGTH)).queue();
            return;
        }

        UUID confessUUID = UUID.randomUUID();
        event.deferReply(true).setContent(Strings.getString("command.confession.submitted").formatted(confession)).queue();

        MessageCreateBuilder embedBuilder = MessageCreateBuilder.from(MessageCreateData.fromEmbeds(
                new EmbedBuilder()
                        .setColor(Config.getGuildVariable(guildId, "CONFESSION_EMBED_COLOR"))
                        .setDescription(confession)
                        .setTitle("Confession")
                        .setTimestamp(Instant.now())
                        .build()
        ));
        confessions.put(confessUUID, embedBuilder);
        confessionsContent.put(confessUUID, confession);
        confessionAuthor.put(confessUUID, userId);
        MessageCreateBuilder data = MessageCreateBuilder.from(embedBuilder.build())
                                            .addComponents(ActionRow.of(
                                                    Button.primary("validate-confession;" + confessUUID, Emoji.fromUnicode("✅")),
                                                    Button.primary("warn-confession;" + confessUUID, Emoji.fromUnicode("⚠")),
                                                    Button.primary("refuse-confession;" + confessUUID, Emoji.fromUnicode("❌"))
                                            ));
        if(following) {
            if(!lastMainConfessionValidation.containsKey(userId) && !lastFollowingConfessionValidation.containsKey(userId)) {
                event.getHook().editOriginal(Strings.getString("command.confession.continue.no_last_confession_found")).queue();
                return;
            }
            confessFollowing.add(confessUUID);
            if(lastFollowingConfessionValidation.containsKey(userId)) {
                validationChannel.retrieveMessageById(lastFollowingConfessionValidation.get(userId)).queue(lastValidation -> lastValidation.reply(data.build()).queue());
                return;
            }
            validationChannel.retrieveMessageById(lastMainConfessionValidation.get(userId)).queue(lastValidation -> lastValidation.reply(data.build()).queue(m -> lastFollowingConfessionValidation.put(userId, m.getIdLong())));
            return;
        }
        validationChannel.sendMessage(data.build()).queue(m -> lastMainConfessionValidation.put(userId, m.getIdLong()));
    }

    private void sendConfession(UUID uuid, Long guildId) {
        MessageCreateBuilder confession = confessions.get(uuid);
        confessions.remove(uuid);
        if(confession == null) return;

        String confessionChannelId = Config.getGuildVariable(guildId, "CONFESSION_CHANNEL_ID");
        if(confessionChannelId.isBlank()) {
            MessageUtil.sendAdminMessage(Strings.getString("command.confession.channel_id_invalid"), guildId);
            return;
        }

        TextChannel confessionChannel = Main.getJDA().getChannelById(TextChannel.class, confessionChannelId);
        if(confessionChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("command.confession.channel_id_invalid"), guildId);
            return;
        }
        if(confessFollowing.contains(uuid)) {
            confessionChannel.retrieveMessageById(lastMainConfession.get(confessionAuthor.get(uuid))).queue(m -> {
                ThreadChannel channel = m.getStartedThread();
                if(channel == null) {
                    m.createThreadChannel("Confession - Follow").queue(t -> {
                        if(t == null) {
                            MessageUtil.sendAdminMessage(Strings.getString("command.confession.channel_id_invalid"), guildId);
                            return;
                        }
                        t.sendMessage(confession.build()).queue();
                    });
                    return;
                }
                channel.sendMessage(confession.build()).queue();
            });
            confessFollowing.remove(uuid);
            return;
        }
        Long authorId = confessionAuthor.get(uuid);
        confessionAuthor.remove(uuid);
        lastFollowingConfessionValidation.remove(authorId);
        confessionChannel.sendMessage(confession.build()).queue(m -> lastMainConfession.put(authorId, m.getIdLong()));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getButton().getCustomId();
        if(id == null || !id.contains(CONFESSION)) return;
        if(event.getGuild() == null || event.getGuild().getIdLong() != guildId) return;
        event.getInteraction().deferEdit().queue();
        event.getHook().editOriginalComponents(Collections.emptyList()).queue();
        UUID uuid = UUID.fromString(id.split(";")[1]);
        if(id.startsWith("validate")) {
            updateValidationEmbedColor(VALID, event.getHook(), event.getMessage());
            sendConfession(uuid, event.getGuild().getIdLong());
        } else if(id.startsWith("refuse")){
            updateValidationEmbedColor(REFUSED, event.getHook(), event.getMessage());
            confessions.remove(uuid);
            confessionAuthor.remove(uuid);
            confessFollowing.remove(uuid);
        } else {
            updateValidationEmbedColor(WARNED, event.getHook(), event.getMessage());
            warn(event.getUser().getIdLong(), uuid);
        }
        confessionsContent.remove(uuid);
    }

    private void warn(Long moderatorId, UUID uuid) {
        Long confessionAuthorId = confessionAuthor.get(uuid);
        confessionAuthor.remove(uuid);
        WarnedConfession warnedConfession = new WarnedConfession(moderatorId, confessionAuthorId, confessionsContent.get(uuid) , Timestamp.from(Instant.now()));
        warnedConfessionRepository.create(warnedConfession);
        int threshold = Config.<Integer>getGuildVariable(guildId, "CONFESSION_WARN_THRESHOLD");
        Guild guild = Main.getJDA().getGuildById(guildId);
        if(guild == null) return;

        List<WarnedConfession> authorWarnedConfessions = warnedConfessionRepository.readByAuthor(confessionAuthorId);
        Main.getJDA().retrieveUserById(confessionAuthorId).flatMap(User::openPrivateChannel).queue(privateChannel -> privateChannel.sendMessage(Strings.getString("command.confession.warn_author")).queue());

        if(authorWarnedConfessions.size() < threshold) return;
        TextChannel validationChannel = Main.getJDA().getChannelById(TextChannel.class, Config.getGuildVariable(guildId,"CONFESSION_VALIDATION_CHANNEL_ID"));
        if(validationChannel == null) {
            MessageUtil.sendAdminMessage(Strings.getString("command.confession.validation_channel_id_invalid"), guildId);
            return;
        }
        Member member = guild.getMemberById(confessionAuthorId);
        String username = Optional.ofNullable(Main.getJDA().getUserById(confessionAuthorId)).map(User::getName).orElse("USER NOT ON SERVER: " + confessionAuthorId);
        EmbedBuilder builder = new EmbedBuilder()
                                       .setDescription(Strings.getString("command.confession.too_much_warnings")
                                                               .formatted(
                                                                       member == null ? username : member.getAsMention(),
                                                                       threshold,
                                                                       threshold,
                                                                       member == null ? "@" + username : "@" + member.getEffectiveName()
                                                               )
                                       )
                                       .setColor(Color.ORANGE);
        validationChannel.sendMessageEmbeds(builder.build()).queue(m -> {
            for (WarnedConfession confession : authorWarnedConfessions) {
                Member mod = guild.getMemberById(confession.moderatorId());
                EmbedBuilder warned = new EmbedBuilder()
                                              .setFooter(Strings.getString("command.confession.warned_by").formatted(mod == null ? confession.moderatorId() : mod.getUser().getName()))
                                              .setDescription(confession.messageContent())
                                              .setColor(Color.BLACK)
                                              .setTimestamp(confession.timestamp().toInstant())
                                              .setAuthor(member == null ? String.valueOf(confessionAuthorId) : member.getUser().getName());
                m.replyEmbeds(warned.build()).queue();
            }
        });
    }
    private void updateValidationEmbedColor(int state, InteractionHook hook, Message message) {
        MessageEmbed embed = message.getEmbeds().getFirst();
        EmbedBuilder builder = EmbedBuilder.fromData(embed.toData()).setColor(VALIDATION_EMBED_COLORS[state]).setTitle(VALIDATION_EMBED_TITLES[state]);
        hook.editOriginalEmbeds(builder.build()).queue();
    }

    public void clearWarnings(long authorId) {
        warnedConfessionRepository.deleteByAuthor(authorId);
    }

}
