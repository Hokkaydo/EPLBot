package com.github.hokkaydo.eplbot.module.code;
import com.github.hokkaydo.eplbot.Config;
import com.github.hokkaydo.eplbot.Strings;
import com.github.hokkaydo.eplbot.command.Command;
import com.github.hokkaydo.eplbot.command.CommandContext;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
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


public class CodeCommand extends ListenerAdapter implements Command{
    private static final String TEMP_DIR = System.getProperty("user.dir")+File.separator+"src"+File.separator+"temp";
    private static final Map<String, Runner> RUNNER_MAP = Map.of(
            "python", new PythonRunner(),
            "rust", new RustCompiler(),
            "java", new JavaRunner()
        );
    private static final long MAX_SENT_FILE_SIZE = 8L * 1024 * 1024; 
    @Override
    public void executeCommand(CommandContext context) {
        Guild guild = context.interaction().getGuild();
        if(guild == null) return;
        if (context.options().size() <= 1) {
            context.interaction().replyModal(Modal.create(context.author().getId() + "submitCode","Execute du code")
                                                     .addActionRow(TextInput.create("language", "Language", TextInputStyle.PARAGRAPH).setPlaceholder("python|rust|java").setRequired(true).build())
                                                     .addActionRow(TextInput.create("body", "Code", TextInputStyle.PARAGRAPH).setPlaceholder("Code").setRequired(true).build())
                                                     .build()).queue();
            return;
        } 
        context.replyCallbackAction().setContent("Processing since: <t:" + Instant.now().getEpochSecond() + ":R>").setEphemeral(false).queue();
        context.options()
                .get(0)
                .getAsAttachment()
                .getProxy()
                .downloadToFile(new File(("%s"+File.pathSeparator+"input.txt").formatted(TEMP_DIR)))
                .thenAcceptAsync(file -> {
                    String content = readFromFile(file).orElse("");
                    Runner runner = RUNNER_MAP.get(context.options().get(1).getAsString());
                    String result = runner.run(content, Config.getGuildVariable(guild.getIdLong(), "COMMAND_CODE_TIMELIMIT"));
                    performSubmit(context.channel(),content, context.options().get(1).getAsString());
                    performResponse(context.channel(),result);
                    file.delete();
                })
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
        
    }
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if(event.getInteraction().getType() != InteractionType.MODAL_SUBMIT || !event.getModalId().contains("Code")) return;
        Optional<ModalMapping> lang = Optional.ofNullable(event.getInteraction().getValue("language"));
        Optional<ModalMapping> body = Optional.ofNullable(event.getInteraction().getValue("body"));
        Guild guild = event.getGuild();
        if(lang.isEmpty() || body.isEmpty() || guild == null) return;

        Integer runTimeout = Config.getGuildVariable(guild.getIdLong(), "COMMAND_CODE_TIMELIMIT");
        event.getInteraction().reply("Processing since: <t:" + Instant.now().getEpochSecond() + ":R>").queue();

        String languageOption = Objects.requireNonNull(lang.get().getAsString());
        String bodyStr = Objects.requireNonNull(body.get().getAsString());
        Runner runner = RUNNER_MAP.get(languageOption);
        performSubmit(event.getMessageChannel(),bodyStr, languageOption);
        performResponse(event.getMessageChannel(),runner.run(bodyStr, runTimeout));
    }
    private void performSubmit(MessageChannel textChannel, String bodyStr,String codeName){
        if (!validateMessageLength(bodyStr)){
            sendWrappedCodeAsText(textChannel,bodyStr,codeName);
            return;
        }
        File submitted = createWrappedCodeAsFile(bodyStr);
        if (submitted == null){
            textChannel.sendMessage(Strings.getString("COMMAND_CODE_COULDNT_WRITE_THE_FILE")).queue();
            return;
        }
        if (!validateFileSize(submitted)){
            sendSubmittedAsFile(textChannel,submitted);
            return;
        }
        textChannel.sendMessage(Strings.getString("COMMAND_CODE_EXCEEDED_FILE_SIZE")).queue();
    }
    private void performResponse(MessageChannel textChannel, String result){
        if (!validateMessageLength(result)){
            textChannel.sendMessage("`"+result+"`").queue();
            return;
        }
        File submitted = createResponseAsFile(result);
        if (submitted == null){
            textChannel.sendMessage(Strings.getString("COMMAND_CODE_COULDNT_WRITE_THE_FILE")).queue();
            return;
        }
        if (!validateFileSize(submitted)){
            sendResponseAsFile(textChannel,submitted);
            return;
        }
        textChannel.sendMessage(Strings.getString("COMMAND_CODE_EXCEEDED_FILE_SIZE")).queue();
    }
    private void sendWrappedCodeAsText(MessageChannel textChannel, String bodyStr, String codeName){
        textChannel.sendMessage("```"+codeName.toLowerCase()+"\n"+bodyStr+"\n```").queue();
    }
    private boolean validateMessageLength(String content){
        return content.length() >= 2000;
    }
    private File createWrappedCodeAsFile(String bodyStr){
        try {
            FileWriter myWriter = new FileWriter(("%s"+File.separator+"responseCode.txt").formatted(TEMP_DIR));
            myWriter.write(bodyStr);
            myWriter.close();
            return new File(("%s"+File.separator+"responseCode.txt").formatted(TEMP_DIR));;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        } 
    }
    private void sendSubmittedAsFile(MessageChannel textChannel,File serverFile){
        FileUpload file = FileUpload.fromData(serverFile,"responseCode.txt");
        textChannel.sendFiles(file).queue(s -> serverFile.delete());
    }
    private File createResponseAsFile(String result){
        try {
            FileWriter myWriter = new FileWriter(("%s"+File.separator+"result.txt").formatted(TEMP_DIR));
            myWriter.write(result);
            myWriter.close();
            return new File(("%s"+File.separator+"result.txt").formatted(TEMP_DIR));
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }
    private void sendResponseAsFile(MessageChannel textChannel, File serverFile){
        FileUpload file = FileUpload.fromData(serverFile,"result.txt");
        textChannel.sendFiles(file).queue(s -> serverFile.delete());
    }
    private boolean validateFileSize(File file){
        long fileSizeInBytes = file.length();
        return fileSizeInBytes > MAX_SENT_FILE_SIZE;
    }
    
    private Optional<String> readFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return Optional.of(reader.lines().collect(Collectors.joining(System.lineSeparator())));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
    @Override
    public String getName() {
        return "compile";
    }
    @Override
    public Supplier<String> getDescription() {
        return () -> Strings.getString("COMMAND_CODE_DESCRIPTION");
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

