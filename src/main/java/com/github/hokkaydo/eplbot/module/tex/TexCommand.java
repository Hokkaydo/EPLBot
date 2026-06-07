package com.github.hokkaydo.eplbot.module.tex;

import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.module.preferences.UserPreferencesStore;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TexCommand extends ListenerAdapter implements Command {

    private static final String MODAL_ID_PREFIX = "tex-modal-";
    private static final long SAVED_TTL_MINUTES = 5;

    // Survives across guild instances (static) — keyed by globally unique Discord user IDs
    private static final Map<Long, String>             SAVED_CONTENT = new ConcurrentHashMap<>();
    private static final Map<Long, ScheduledFuture<?>> SAVED_TTL     = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService      TTL_SCHEDULER = new ScheduledThreadPoolExecutor(1);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(TTL_SCHEDULER::shutdown));
    }

    private final long guildId;

    TexCommand(long guildId) {
        this.guildId = guildId;
    }

    // Store content for 5 min; cancels any existing timer for this user first
    private static void saveContent(long userId, String content) {
        ScheduledFuture<?> old = SAVED_TTL.remove(userId);
        if (old != null) old.cancel(false);
        SAVED_CONTENT.put(userId, content);
        SAVED_TTL.put(userId, TTL_SCHEDULER.schedule(() -> {
            SAVED_CONTENT.remove(userId);
            SAVED_TTL.remove(userId);
        }, SAVED_TTL_MINUTES, TimeUnit.MINUTES));
    }

    private static void clearContent(long userId) {
        SAVED_CONTENT.remove(userId);
        ScheduledFuture<?> ttl = SAVED_TTL.remove(userId);
        if (ttl != null) ttl.cancel(false);
    }

    @Override
    public void executeCommand(CommandContext context) {
        long userId  = context.user().getIdLong();
        String modalId = MODAL_ID_PREFIX + userId;

        String saved = SAVED_CONTENT.get(userId);
        TextInput.Builder inputBuilder = TextInput.create("tex_body", TextInputStyle.PARAGRAPH)
                .setPlaceholder(Strings.getString("command.tex.modal.placeholder"))
                .setRequired(true)
                .setMaxLength(4000);
        if (saved != null) inputBuilder.setValue(saved);

        context.interaction().replyModal(
                Modal.create(modalId, Strings.getString("command.tex.modal.title"))
                        .addComponents(Label.of(Strings.getString("command.tex.modal.label"), inputBuilder.build()))
                        .build()
        ).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        var guild = event.getGuild();
        if (guild == null || guild.getIdLong() != guildId) return;
        if (!event.getModalId().startsWith(MODAL_ID_PREFIX)) return;

        Optional<ModalMapping> bodyOpt = Optional.ofNullable(event.getValue("tex_body"));
        if (bodyOpt.isEmpty()) {
            event.reply("No LaTeX content provided.").setEphemeral(true).queue();
            return;
        }

        var member = event.getMember();
        if (member == null) return;
        var channel = event.getChannel().asGuildMessageChannel();

        long userId  = member.getIdLong();
        String content = bodyOpt.get().getAsString();
        boolean dark   = UserPreferencesStore.isDarkTheme(userId, guild.getIdLong());

        event.deferReply(true).queue(hook ->
                LatexRenderer.renderToImage(content, dark)
                        .thenAccept(imageBytes -> {
                            // Success: forget saved draft
                            clearContent(userId);
                            new RenderedTex(channel, member).send(imageBytes, null, null, msg ->
                                    hook.deleteOriginal().queue(null, _ -> {})
                            );
                        })
                        .exceptionally(t -> {
                            // Failure: store content so next /tex pre-fills the modal
                            saveContent(userId, content);
                            Throwable cause = t.getCause() != null ? t.getCause() : t;
                            if (cause instanceof LatexRenderer.LatexCompilationException) {
                                String log = cause.getMessage();
                                String[] lines = log.split("\n");
                                int start = Math.max(0, lines.length - 15);
                                StringBuilder excerpt = new StringBuilder();
                                for (int i = start; i < lines.length; i++) excerpt.append(lines[i]).append('\n');
                                String trimmed = excerpt.toString().strip();
                                if (trimmed.length() > 1800) trimmed = trimmed.substring(trimmed.length() - 1800);
                                hook.editOriginal("❌ LaTeX compilation error:\n```\n" + trimmed + "\n```").queue();
                            } else {
                                hook.editOriginal(Strings.getString("command.tex.render_error")).queue();
                            }
                            return null;
                        })
        );
    }

    @Override
    public String getName() {
        return "tex";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.tex.description");
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public boolean ephemeralReply() {
        return false;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return true;
    }

    @Override
    public boolean adminOnly() {
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("command.tex.help");
    }
}
