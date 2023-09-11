package com.github.hokkaydo.eplbot.module.code;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;
import net.dv8tion.jda.api.entities.Message;
import com.github.hokkaydo.eplbot.module.code.JavaLoader.JavaRunner;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.util.function.Supplier;
import java.lang.reflect.Method;
import java.io.FileWriter; 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.time.Instant;
import java.util.Scanner;

public class JavaCommand extends ListenerAdapter implements Command {
    
    @Override
    public void executeCommand(CommandContext context) {
        if (context.options().size() == 0) {
            String key = context.author().getId() + "javaCode";
            Modal modal = Modal.create(key,"Execute du code java")
                .addActionRow(TextInput.create("body", "Code", TextInputStyle.PARAGRAPH).setPlaceholder("Corps").setRequired(true).build())
                .build();
            context.interaction().replyModal(modal).queue();
        } else {
            long unixTimestamp = Instant.now().getEpochSecond();
            context.replyCallbackAction().setContent("Processing since: <t:" + unixTimestamp + ":R>").setEphemeral(false).queue();
            Attachment attachment = context.options().get(0).getAsAttachment();
            attachment.downloadToFile()
                .thenAcceptAsync(file -> {
                    try {
                        String content = readFromFile(file);
                        file.delete();
                        JavaRunner runner = new JavaRunner();
                        String result = runner.javaParse(content);
                        MessageChannel textChannel = context.channel();
                        messageLengthCheck(textChannel,content,result);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
            
        }
    }
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if(event.getInteraction().getType() != InteractionType.MODAL_SUBMIT || !event.getModalId().contains("javaCode")) return;
            ReplyCallbackAction callbackAction = event.deferReply(true);   
            String bodyStr = Objects.requireNonNull(event.getInteraction().getValue("body")).getAsString();
            long unixTimestamp = Instant.now().getEpochSecond();
            callbackAction.setContent("Processing since: <t:" + unixTimestamp + ":R>").setEphemeral(false).queue();
            JavaRunner runner = new JavaRunner();
            String result = runner.javaParse(bodyStr);
            MessageChannel textChannel = event.getMessageChannel();
            messageLengthCheck(textChannel,bodyStr,result);

    }
    private void messageLengthCheck(MessageChannel textChannel, String bodyStr, String result){
        try {
            textChannel.sendMessage("```java\n"+bodyStr+"\n```").queue();
        } catch (IllegalArgumentException error1){
            try {
                FileWriter myWriter = new FileWriter(System.getProperty("user.dir")+"/src/main/java/com/github/hokkaydo/eplbot/code/JavaLoader/temp/responseCode.java");
                myWriter.write(bodyStr);
                myWriter.close();
                File serverFile = new File(System.getProperty("user.dir")+"/src/main/java/com/github/hokkaydo/eplbot/code/JavaLoader/temp/responseCode.java");
                FileUpload file = FileUpload.fromData(serverFile,"responseCode.java");
                textChannel.sendFiles(file).queue(s -> serverFile.delete());
              } catch (IOException error3) {
                error1.printStackTrace();
              } catch (Exception e) {
                textChannel.sendMessage("the file given exeeded 8mb");
            }       
        }
        try {
            textChannel.sendMessage("`"+result+"`").queue();
        } catch (IllegalArgumentException error2){
            try {
                FileWriter myWriter = new FileWriter(System.getProperty("user.dir")+"/src/main/java/com/github/hokkaydo/eplbot/code/JavaLoader/temp/result.java");
                myWriter.write(result);
                myWriter.close();
                File serverFile = new File(System.getProperty("user.dir")+"/src/main/java/com/github/hokkaydo/eplbot/code/JavaLoader/temp/result.java");
                FileUpload file = FileUpload.fromData(serverFile,"result.java");
                textChannel.sendFiles(file).queue(s -> serverFile.delete());


              } catch (IOException error3) {    
                error2.printStackTrace();
              } catch (Exception e) {
                textChannel.sendMessage("the file produced exeeded 8mb");
            }       
        }
    }
    private String readFromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
    @Override
    public String getName() {
        return "java";
    }
    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_RUN_JAVA_DESC");
    }
    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.ATTACHMENT, "file", Strings.getString("COMMAND_RUN_JAVA_FILE_OPTION_DESCRIPTION"), false)
        );
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
        return () -> Strings.getString("COMMAND_RUN_JAVA_HELP");
    }

}
