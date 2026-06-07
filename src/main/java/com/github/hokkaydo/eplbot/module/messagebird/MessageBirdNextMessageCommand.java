package com.github.hokkaydo.eplbot.module.messagebird;

import com.github.hokkaydo.eplbot.MessageUtil;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class MessageBirdNextMessageCommand extends ListenerAdapter implements Command {

    private final long guildId;
    private final List<String> types;

    MessageBirdNextMessageCommand(long guildId, List<String> types) {
        this.guildId = guildId;
        this.types = types;
    }

    @Override
    public void executeCommand(CommandContext context) {
        if (!context.interaction().isGuildCommand() || context.interaction().getGuild() == null) return;
        String type = context.getOption("type").map(OptionMapping::getAsString).orElse(null);
        String roleId = Config.getGuildVariable(guildId, type + "_BIRD_ROLE_ID");
        if (context.author().getRoles().stream().filter(r -> r.getId().equals(roleId)).findFirst().isEmpty()) {
            context.replyCallbackAction().setContent(Strings.getString("command.message_bird.not_valid_bird")).queue();
            return;
        }
        Modal modal = Modal.create("messagebird-" + type, "Message")
                              .addComponents(Label.of("Message", TextInput.create("message", TextInputStyle.PARAGRAPH).build()))
                              .build();
        context.interaction().replyModal(modal).queue();
    }

    @Override
    public String getName() {
        return "messagebird";
    }

    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("command.message_bird.next_message.description");
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "type", "Type", true)
                        .addChoices(types.stream().map(s -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(s, s)).toList())
        );

    }

    @Override
    public boolean ephemeralReply() {
        return true;
    }

    @Override
    public boolean validateChannel(MessageChannel channel) {
        return channel instanceof GuildMessageChannel;
    }

    @Override
    public boolean adminOnly() {
        return false;
    }

    @Override
    public Supplier<String> help() {
        return () -> Strings.getString("command.message_bird.next_message.help");
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getGuild() == null || event.getGuild().getIdLong() != guildId) return;
        String id = event.getModalId();
        if (!id.startsWith("messagebird")) return;
        String type = id.substring("messagebird-".length());
        ModalMapping contentMap = event.getInteraction().getValue("message");
        if (contentMap == null) return;
        String content = contentMap.getAsString();
        if (content.length() > Message.MAX_CONTENT_LENGTH) {
            event.reply(Strings.getString("command.message_bird.message_too_long").formatted(Message.MAX_CONTENT_LENGTH)).queue();
            return;
        }
        Config.updateValue(event.getGuild().getIdLong(), type + "_BIRD_NEXT_MESSAGE", content);
        MessageUtil.sendAdminMessage("Prochain message %s enregistré par %s :%n >>> %s".formatted(type, event.getUser().getAsMention(), content), event.getGuild().getIdLong());
        event.reply(Strings.getString("command.message_bird.next_message.registered")).queue();
        event.getUser().openPrivateChannel().queue(dm -> dm.sendMessage(Strings.getString("command.message_bird.next_message.registered_dm").formatted(content)).queue());
    }

}
