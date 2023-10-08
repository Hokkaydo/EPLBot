package com.github.hokkaydo.eplbot.module.code;
import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import java.util.function.Supplier;
import java.io.FileWriter; 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

import java.lang.reflect.InvocationTargetException;

public class CodeCommand extends ListenerAdapter implements Command{
    private final Map<String, Runner> RUNNER_MAP = new HashMap<>();
    public CodeCommand() {
        RUNNER_MAP.put("python", new PythonRunner());
        RUNNER_MAP.put("rust", new RustCompiler());
        RUNNER_MAP.put("java", new JavaRunner());
    }

    @Override
    public void executeCommand(CommandContext context) {
        if (context.options().size() <= 1) {
            context.interaction().replyModal(Modal.create(context.author().getId() + "submitCode","Execute du code")
            .addActionRow(TextInput.create("language", "Language", TextInputStyle.PARAGRAPH).setPlaceholder("python|rust|java").setRequired(true).build())
            .addActionRow(TextInput.create("body", "Code", TextInputStyle.PARAGRAPH).setPlaceholder("Code").setRequired(true).build())
            .build()).queue();
        } else {
            
            context.replyCallbackAction().setContent("Processing since: <t:" + Instant.now().getEpochSecond() + ":R>").setEphemeral(false).queue();
            context.options().get(0).getAsAttachment().getProxy().downloadToFile(new File(System.getProperty("user.dir")+"\\src\\temp\\input.txt"))
                    .thenAcceptAsync(file -> {
                        try {
                            String content = readFromFile(file);
                            Runner runner = RUNNER_MAP.get(context.options().get(1));
                            messageLengthCheck(context.channel(), content, (String) runner.run(content, Config.getGuildVariable(Long.parseLong(context.interaction().getGuild().getId()), "COMMAND_CODE_TIMELIMIT")),context.options().get(1).getAsString());
                            file.delete();
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
        if(event.getInteraction().getType() != InteractionType.MODAL_SUBMIT || !event.getModalId().contains("Code")) return;
            Integer runTimeout = Config.getGuildVariable(Long.parseLong(event.getGuild().getId()), "COMMAND_CODE_TIMELIMIT");
            event.deferReply(true).setContent("Processing since: <t:" + Instant.now().getEpochSecond() + ":R>").setEphemeral(false).queue();  
            String languageOption = Objects.requireNonNull(event.getInteraction().getValue("language").getAsString());
            String bodyStr = Objects.requireNonNull(event.getInteraction().getValue("body")).getAsString();
            Runner runner = RUNNER_MAP.get(languageOption);
            messageLengthCheck(event.getMessageChannel(),bodyStr,(String) runner.run( bodyStr, runTimeout),languageOption);
    }
    private void messageLengthCheck(MessageChannel textChannel, String bodyStr, String result,String codeName){
        try {
            textChannel.sendMessage("```"+codeName.toLowerCase()+"\n"+bodyStr+"\n```").queue();
        } catch (IllegalArgumentException error1){
            try {
                FileWriter myWriter = new FileWriter(System.getProperty("user.dir")+"\\src\\temp\\responseCode.txt");
                myWriter.write(bodyStr);
                myWriter.close();
                File serverFile = new File(System.getProperty("user.dir")+"\\src\\temp\\responseCode.txt");
                FileUpload file = FileUpload.fromData(serverFile,"responseCode.txt");
                textChannel.sendFiles(file).queue(s -> serverFile.delete());
              } catch (IOException error3) {
                error1.printStackTrace();
              } catch (Exception e) {
                textChannel.sendMessage(Strings.getString("COMMAND_CODE_EXCEEDEDFILESIZE"));
            }       
        }
        try {
            textChannel.sendMessage("`"+result+"`").queue();
        } catch (IllegalArgumentException error2){
            try {
                FileWriter myWriter = new FileWriter(System.getProperty("user.dir")+"\\src\\temp\\result.txt");
                myWriter.write(result);
                myWriter.close();
                File serverFile = new File(System.getProperty("user.dir")+"\\src\\temp\\result.txt");
                FileUpload file = FileUpload.fromData(serverFile,"result.txt");
                textChannel.sendFiles(file).queue(s -> serverFile.delete());


              } catch (IOException error3) {    
                error3.printStackTrace();
              } catch (Exception e) {
                textChannel.sendMessage(Strings.getString("COMMAND_CODE_EXCEEDEDFILESIZE"));
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
        return "compile";
    }
    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CODE_DESC");
    }
    @Override
    public List<OptionData> getOptions() {
        return List.of(

            new OptionData(OptionType.ATTACHMENT, "file", Strings.getString("COMMAND_CODE_FILE_OPTION_DESCRIPTION"), false),
            new OptionData(OptionType.STRING, "language", Strings.getString("COMMAND_CODE_LANG_OPTION_DESCRIPTION"), false)
            .addChoice("python", "python")
            .addChoice("rust", "rust")
            .addChoice("java", "java")

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
        return () -> Strings.getString("COMMAND_CODE_HELP");
    }

}

