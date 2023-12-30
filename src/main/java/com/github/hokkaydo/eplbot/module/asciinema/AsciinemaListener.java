package com.github.hokkaydo.eplbot.module.asciinema;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.AttachmentProxy;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class AsciinemaListener extends ListenerAdapter {

    private static final Random RANDOM = new Random();


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.isEmpty()) return; // no attachments on the message!
        for (Message.Attachment att: attachments) {
            //not interested in other files than cast ones.
            if (!Objects.equals(att.getFileExtension(), "cast")) continue;

            //generating random filename to store .cast (and then .gif)
            int rnd = Math.abs(RANDOM.nextInt());
            String fileName = rnd+".cast";
            String gifFileName = rnd+".gif";

            //Downloading cast file from discord message.
            try {
                (new AttachmentProxy(att.getUrl())).downloadToFile(new File(fileName)).get();
            } catch (InterruptedException | ExecutionException e) {
                try {
                    assert (new File(fileName)).delete();
                } catch (Exception ignored) {}
                throw new RuntimeException(e);
            }

            //convert to gif using agg command
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("agg", fileName, gifFileName);
            try {
                builder.start().waitFor();
            } catch (InterruptedException | IOException e) {
                assert (new File(fileName)).delete();
                throw new RuntimeException(e);
            }

            //creating message to send back
            File gifFile = new File(gifFileName);
            Member author = event.getMember();
            // this message will contain the gif, as well as author and text originally attached to .cast file
            // I am doing that because I will then remove original user message
            MessageEmbed message = new EmbedBuilder()
                    .setAuthor(Objects.requireNonNull(author).getEffectiveName(), null, author.getEffectiveAvatarUrl())
                    .appendDescription(event.getMessage().getContentDisplay())
                    .build();

            //deleting original message, then sending message
            event.getMessage().delete().queue();
            event.getChannel().sendMessageEmbeds(message).queue();
            event.getChannel().sendFiles(FileUpload.fromData(gifFile)).queue(m -> {
                //cleaning up.
                (new File(gifFileName)).delete();
                (new File(fileName)).delete();

            });


        }
    }


}
