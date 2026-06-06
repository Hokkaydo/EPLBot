package com.github.hokkaydo.eplbot.module.translation;

import com.github.hokkaydo.eplbot.configuration.Config;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TranslationListener extends ListenerAdapter {

    private final TranslationModule translationModule;
    TranslationListener(TranslationModule translationModule) {
        this.translationModule = translationModule;
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        TranslatedMessage.evictChannel(event.getChannel().getIdLong());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if(event.getGuild().getIdLong() != translationModule.getGuildId()) return;
        if(event.getMessage().isWebhookMessage()) return;
        if (event.getMessage().getContentRaw().isBlank()) return;
        if (!translationModule.isEnabled()) return;

        String lang = Config.getGuildVariable(event.getGuild().getIdLong(), "TRANSLATION_LANGUAGE");

        String message = event.getMessage().getContentRaw();
        Optional<String> traduction = TranslationModule.translate(message, TranslationModule.FR, lang);
        if (traduction.isEmpty()) return;
        TranslatedMessage traduced = new TranslatedMessage(event.getMessage());
        Message referenced = event.getMessage().getReferencedMessage();
        traduced.createAndSendMessage(referenced, traduction.get(), _ -> event.getMessage().delete().queue());
    }

}
