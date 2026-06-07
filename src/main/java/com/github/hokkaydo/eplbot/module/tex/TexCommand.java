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
import java.util.Optional;
import java.util.function.Supplier;

public class TexCommand extends ListenerAdapter implements Command {

    private static final String MODAL_ID_PREFIX = "tex-modal-";

    private final long guildId;

    TexCommand(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public void executeCommand(CommandContext context) {
        String modalId = MODAL_ID_PREFIX + context.user().getId();

        TextInput latexInput = TextInput.create("tex_body", TextInputStyle.PARAGRAPH)
                .setPlaceholder(Strings.getString("command.tex.modal.placeholder"))
                .setRequired(true)
                .setMaxLength(4000)
                .build();

        context.interaction().replyModal(
                Modal.create(modalId, Strings.getString("command.tex.modal.title"))
                        .addComponents(Label.of(Strings.getString("command.tex.modal.label"), latexInput))
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

        String content = bodyOpt.get().getAsString();
        boolean dark = UserPreferencesStore.isDarkTheme(member.getIdLong(), guild.getIdLong());

        // Defer ephemerally so Discord doesn't time out; result arrives via webhook
        event.deferReply(true).queue(hook ->
                LatexRenderer.renderToImage(content, dark)
                        .thenAccept(imageBytes ->
                                new RenderedTex(channel, member).send(imageBytes, null, null, msg ->
                                        hook.deleteOriginal().queue(null, _ -> {})
                                )
                        )
                        .exceptionally(t -> {
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
